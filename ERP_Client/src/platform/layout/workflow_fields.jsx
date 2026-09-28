import { useEffect, useRef, useState } from 'react';
import { Form, Select } from 'antd';
import { request_api } from '../common/api_client';
import { to_iso_date } from '../common/formatters';

const lookup_cache = new Map();
const lookup_requests = new Map();

const lookup_definitions = {
  employees: {
    endpoint: '/api/v1/human_resources/employees/lookup?limit=200',
    remote_search: true,
    map_item: (item) => ({ value: item.employee_id, label: item.employee_code + ' — ' + item.full_name }),
  },
  departments: {
    endpoint: '/api/v1/human_resources/master_data/departments?status=active&page=0&page_size=200',
    map_item: (item) => ({ value: item.department_id, label: item.department_code + ' — ' + item.department_name }),
  },
  job_titles: {
    endpoint: '/api/v1/human_resources/master_data/job_titles?status=active&page=0&page_size=200',
    map_item: (item) => ({ value: item.job_title_id, label: item.job_title_code + ' — ' + item.job_title_name }),
  },
  work_shifts: {
    endpoint: '/api/v1/human_resources/master_data/work_shifts/active',
    map_item: (item) => ({ value: item.work_shift_id, label: item.shift_code + ' — ' + item.shift_name }),
  },
  warehouses: {
    endpoint: '/api/v1/inventory/master_data/warehouses',
    map_item: (item) => ({ value: item.warehouse_id, label: item.warehouse_code + ' — ' + item.warehouse_name }),
  },
  warehouse_locations: {
    map_item: (item) => ({ value: item.warehouse_location_id, label: item.location_code + ' — ' + item.location_name }),
  },
  suppliers: {
    endpoint: '/api/v1/inventory/master_data/suppliers',
    map_item: (item) => ({ value: item.supplier_id, label: item.supplier_code + ' — ' + item.supplier_name }),
  },
  raw_materials: {
    endpoint: '/api/v1/inventory/materials?page=0&page_size=1000&status=active&item_type=raw_material',
    map_item: (item) => ({ value: item.stock_item_id, label: item.item_code + ' — ' + item.item_name }),
  },
  finished_products: {
    endpoint: '/api/v1/inventory/materials?page=0&page_size=1000&status=active&item_type=finished_product',
    map_item: (item) => ({ value: item.stock_item_id, label: item.item_code + ' — ' + item.item_name }),
  },
  boms: {
    endpoint: '/api/v1/production/boms?page=0&page_size=200&status=active',
    map_item: (item) => ({ value: item.bom_id, label: item.bom_code + ' · v' + item.version_number + ' — ' + item.product_item_name }),
  },
  production_orders: {
    endpoint: '/api/v1/production/orders?page=0&page_size=200',
    map_item: (item) => ({ value: item.production_order_id, label: item.order_code + ' — ' + item.stock_item_name }),
  },
  production_plans: {
    endpoint: '/api/v1/production/plans?page=0&page_size=200',
    map_item: (item) => ({ value: item.production_plan_id, label: item.plan_code + ' — ' + item.plan_name + ' (' + item.status + ')' }),
  },
};

function response_items(response) {
  const data = response?.data;
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.items)) return data.items;
  return [];
}

async function fetch_lookup(lookup_key, endpoint, force_refresh = false) {
  const cache_key = lookup_key + '|' + endpoint;
  const definition = lookup_definitions[lookup_key] || {};
  const cacheable = !definition.remote_search;
  if (force_refresh) lookup_cache.delete(cache_key);
  if (cacheable && lookup_cache.has(cache_key)) return lookup_cache.get(cache_key);
  if (lookup_requests.has(cache_key)) return lookup_requests.get(cache_key);
  const request = fetch_all_items(endpoint).then((items) => {
    const options = items.map((item) => definition.map_item ? definition.map_item(item) : item).filter((item) => item?.value !== undefined);
    if (cacheable) lookup_cache.set(cache_key, options);
    return options;
  }).finally(() => lookup_requests.delete(cache_key));
  lookup_requests.set(cache_key, request);
  return request;
}

function page_endpoint(endpoint, page) {
  if (!endpoint || !/[?&]page=\d+/.test(endpoint)) return null;
  return endpoint.replace(/([?&]page=)\d+/, '$1' + page);
}

async function fetch_all_items(endpoint) {
  const first_response = await request_api(endpoint);
  const first_items = response_items(first_response);
  const total_pages = Number(first_response?.data?.total_pages || 0);
  if (total_pages <= 1) return first_items;
  const pages = await Promise.all(Array.from({ length: total_pages - 1 }, (_, index) => {
    const next_endpoint = page_endpoint(endpoint, index + 1);
    return next_endpoint ? request_api(next_endpoint).then(response_items) : [];
  }));
  const seen = new Set();
  return [first_items, ...pages].flat().filter((item) => {
    const key = String(item?.id ?? item?.stock_item_id ?? item?.employee_id ?? item?.warehouse_id ?? item?.bom_id ?? JSON.stringify(item));
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function lookup_endpoint(field, parent_value, search_value = '', current_value) {
  if (field.lookup === 'warehouse_locations') return parent_value ? '/api/v1/inventory/master_data/warehouses/' + parent_value + '/locations' : null;
  const definition = lookup_definitions[field.lookup];
  if (!definition) return null;
  if (definition.remote_search && search_value.trim()) {
    const include = current_value ? '&include_employee_id=' + encodeURIComponent(current_value) : '';
    return '/api/v1/human_resources/employees/lookup?limit=200&search=' + encodeURIComponent(search_value.trim()) + include;
  }
  if (field.lookup === 'employees' && current_value) return '/api/v1/human_resources/employees/lookup?limit=200&include_employee_id=' + encodeURIComponent(current_value);
  return definition.endpoint || null;
}

function LookupField({ field, form, selected_option, ...control_props }) {
  const parent_path = field.parent_field || undefined;
  const parent_value = Form.useWatch(parent_path, form);
  const current_value = Form.useWatch(field.name || '__lookup_current__', form);
  const [search_value, set_search_value] = useState('');
  const endpoint = lookup_endpoint(field, parent_value, search_value, current_value);
  const [options, set_options] = useState([]);
  const [loading, set_loading] = useState(false);
  const [load_failed, set_load_failed] = useState(false);
  const parent_value_ref = useRef(parent_value);

  useEffect(() => {
    if (field.parent_field && parent_value_ref.current !== undefined
      && String(parent_value_ref.current ?? '') !== String(parent_value ?? '') && field.name) {
      form.setFieldValue(field.name, undefined);
    }
    parent_value_ref.current = parent_value;
  }, [field.name, field.parent_field, form, parent_value]);

  useEffect(() => {
    let mounted = true;
    if (!field.lookup || !endpoint) {
      set_options(field.options || []);
      set_load_failed(false);
      return () => { mounted = false; };
    }
    set_loading(true);
    set_load_failed(false);
    fetch_lookup(field.lookup, endpoint).then((next_options) => {
      if (!mounted) return;
      set_options(next_options);
      const current = form.getFieldValue(field.name);
      if (field.auto_select_first && (current === undefined || current === null || current === '') && next_options.length === 1) {
        form.setFieldValue(field.name, next_options[0].value);
      }
    }).catch(() => { if (mounted) { set_options([]); set_load_failed(true); } }).finally(() => { if (mounted) set_loading(false); });
    return () => { mounted = false; };
  // Inline modal fields are recreated on every render; endpoint and lookup are
  // the intentional fetch keys, so adding the whole field object would loop.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [endpoint, field.lookup]);

  // A detail endpoint can return a valid foreign-key id that is outside the
  // first page of a lookup. Keep the current option visible so an edit form
  // never falls back to the misleading “Chọn dữ liệu” placeholder.
  const visible_options = selected_option?.value === undefined || selected_option?.value === null
    ? options
    : options.some((option) => String(option.value) === String(selected_option.value))
      ? options
      : [selected_option, ...options];
  const is_remote = Boolean(field.lookup && lookup_definitions[field.lookup]?.remote_search);
  const handle_remote_blur = async () => {
    const normalized_search = search_value.trim().toLowerCase();
    if (normalized_search && field.name) {
      let candidate_options = options;
      if (!candidate_options.length && endpoint) {
        try {
          candidate_options = await fetch_lookup(field.lookup, endpoint);
        } catch {
          candidate_options = [];
        }
      }
      const exact_matches = candidate_options.filter((option) => {
        const label = String(option.label || '').trim().toLowerCase();
        return label === normalized_search
          || label.startsWith(normalized_search + ' — ')
          || label.startsWith(normalized_search + ' - ');
      });
      if (exact_matches.length === 1) {
        form.setFieldValue(field.name, exact_matches[0].value);
      }
    }
    set_search_value('');
  };
  const refresh_lookup = () => {
    if (!field.lookup || !endpoint) return;
    set_loading(true);
    set_load_failed(false);
    fetch_lookup(field.lookup, endpoint, true).then((next_options) => { set_options(next_options); }).catch(() => { set_options([]); set_load_failed(true); }).finally(() => set_loading(false));
  };
  const handle_open_change = (open) => {
    if (open) refresh_lookup();
    control_props.onOpenChange?.(open);
  };
  const handle_change = async (value, option) => {
    control_props.onChange?.(value, option);
    if (!value || !field.related_endpoint || !field.auto_fill) return;
    try {
      const endpoint_path = field.related_endpoint.replace('{id}', encodeURIComponent(value));
      const response = await request_api(endpoint_path);
      const detail = response?.data || {};
      Object.entries(field.auto_fill).forEach(([target, source]) => {
        let next = detail[source];
        if (next && field.auto_fill_datetime?.includes(target) && /^\d{4}-\d{2}-\d{2}$/.test(String(next))) {
          next = String(next) + (target.startsWith('ends') ? 'T17:00' : 'T08:00');
        }
        if (next !== undefined && next !== null) form.setFieldValue(target, next);
      });
    } catch {
      // The selected relation remains valid even when its optional preview cannot be loaded.
    }
  };
  return <Select {...field.select_props} {...control_props} showSearch allowClear={!field.required} optionFilterProp={is_remote ? undefined : 'label'} filterOption={is_remote ? false : undefined} onSearch={is_remote ? set_search_value : undefined} onBlur={is_remote ? handle_remote_blur : undefined} onOpenChange={handle_open_change} onChange={handle_change} options={field.lookup ? visible_options : (field.options || [])} loading={loading} disabled={Boolean(field.lookup && !endpoint)} placeholder={load_failed ? 'Không tải được danh mục' : field.placeholder || 'Chọn dữ liệu'} style={{ width: '100%', ...(field.select_props?.style || {}) }} />;
}


const production_plan_status_labels = {
  draft: 'Bản nháp',
  approved: 'Đã duyệt',
  released: 'Đã phát hành',
};

const production_plan_statuses_for_order = new Set(Object.keys(production_plan_status_labels));

function production_plan_option(item) {
  return {
    value: item.production_plan_id,
    label: item.plan_code + ' — ' + item.plan_name + ' (' + (production_plan_status_labels[item.status] || item.status) + ')',
    status: item.status,
  };
}

function ProductionPlanField({ form, selected_option, ...control_props }) {
  const [options, set_options] = useState([]);
  const [loading, set_loading] = useState(false);
  const [load_failed, set_load_failed] = useState(false);

  useEffect(() => {
    let mounted = true;
    set_loading(true);
    set_load_failed(false);
    Promise.all(Array.from(production_plan_statuses_for_order).map((status) =>
      fetch_all_items('/api/v1/production/plans?status=' + status + '&page=0&page_size=100')
    )).then((responses) => {
      if (!mounted) return;
      const seen = new Set();
      const next_options = responses.flat()
        .filter((item) => production_plan_statuses_for_order.has(item.status))
        .map(production_plan_option)
        .filter((option) => {
          const key = String(option.value);
          if (seen.has(key)) return false;
          seen.add(key);
          return true;
        });
      set_options(next_options);
    }).catch(() => {
      if (mounted) {
        set_options([]);
        set_load_failed(true);
      }
    }).finally(() => {
      if (mounted) set_loading(false);
    });
    return () => { mounted = false; };
  }, []);

  const selected_value = selected_option?.value;
  const visible_options = selected_value === undefined || selected_value === null
    || options.some((option) => String(option.value) === String(selected_value))
    ? options
    : [selected_option, ...options];

  const handle_change = (value) => {
    const previous_value = form.getFieldValue('production_plan_id');
    control_props.onChange?.(value);
    if (String(previous_value ?? '') !== String(value ?? '')) {
      form.setFieldsValue({
        production_plan_line_id: undefined,
        bom_id: undefined,
        target_quantity: undefined,
        planned_starts_on: undefined,
        planned_ends_on: undefined,
        __production_stock_item_id: undefined,
        __production_plan_line_remaining_quantity: undefined,
      });
    }
  };

  return <Select
    {...control_props}
    showSearch
    allowClear
    optionFilterProp="label"
    options={visible_options}
    loading={loading}
    onChange={handle_change}
    placeholder={load_failed ? 'Không tải được kế hoạch' : 'Chọn kế hoạch còn hiệu lực'}
    style={{ width: '100%' }}
  />;
}

function PlanLineField({ form, ...control_props }) {
  const plan_id = Form.useWatch('production_plan_id', form);
  const line_id = Form.useWatch('production_plan_line_id', form);
  const [options, set_options] = useState([]);
  const [loading, set_loading] = useState(false);
  const lines_ref = useRef([]);
  const plan_ref = useRef(null);
  const line_id_ref = useRef(line_id);

  useEffect(() => {
    line_id_ref.current = line_id;
  }, [line_id]);

  useEffect(() => {
    let mounted = true;
    lines_ref.current = [];
    plan_ref.current = null;
    set_options([]);
    if (!plan_id) {
      form.setFieldsValue({
        production_plan_line_id: undefined,
        target_quantity: undefined,
        planned_starts_on: undefined,
        planned_ends_on: undefined,
        __production_stock_item_id: undefined,
        __production_plan_line_remaining_quantity: undefined,
      });
      set_loading(false);
      return () => { mounted = false; };
    }
    set_loading(true);
    request_api('/api/v1/production/plans/' + plan_id).then((response) => {
      if (!mounted) return;
      const plan = response?.data;
      const lines = Array.isArray(plan?.lines) ? plan.lines : [];
      lines_ref.current = lines;
      plan_ref.current = plan;
      const current_line = lines.find((line) => String(line.production_plan_line_id) === String(line_id_ref.current));
      const selectable_lines = lines.filter((line) => Number(line.remaining_quantity ?? line.target_quantity ?? 0) > 0);
      const selected_line = current_line || selectable_lines[0];
      const next_options = selectable_lines.map((line) => {
        const remaining = line.remaining_quantity ?? line.target_quantity;
        return {
          value: line.production_plan_line_id,
          label: line.stock_item_code + ' — ' + line.stock_item_name + ' · còn ' + remaining + ' ' + line.unit_code,
        };
      });
      set_options(next_options);
      const remaining = selected_line ? (selected_line.remaining_quantity ?? selected_line.target_quantity) : undefined;
      form.setFieldsValue({
        production_plan_line_id: selected_line?.production_plan_line_id,
        target_quantity: remaining,
        planned_starts_on: plan?.starts_on,
        planned_ends_on: plan?.ends_on,
        __production_stock_item_id: selected_line?.stock_item_id,
        __production_plan_line_remaining_quantity: remaining,
      });
    }).catch(() => {
      if (mounted) {
        set_options([]);
        form.setFieldsValue({
          production_plan_line_id: undefined,
          target_quantity: undefined,
          __production_stock_item_id: undefined,
          __production_plan_line_remaining_quantity: undefined,
        });
      }
    }).finally(() => {
      if (mounted) set_loading(false);
    });
    return () => { mounted = false; };
  }, [form, plan_id]);

  useEffect(() => {
    const line = lines_ref.current.find((item) => String(item.production_plan_line_id) === String(line_id));
    if (!line) return;
    const remaining = line.remaining_quantity ?? line.target_quantity;
    form.setFieldsValue({
      target_quantity: remaining,
      __production_stock_item_id: line.stock_item_id,
      __production_plan_line_remaining_quantity: remaining,
      planned_starts_on: plan_ref.current?.starts_on,
      planned_ends_on: plan_ref.current?.ends_on,
    });
  }, [form, line_id]);

  return <Select
    {...control_props}
    showSearch
    allowClear
    optionFilterProp="label"
    options={options}
    loading={loading}
    disabled={!plan_id}
    placeholder={plan_id ? 'Chọn dòng còn số lượng' : 'Chọn kế hoạch trước'}
    style={{ width: '100%' }}
  />;
}

function ProductionBomField({ form, selected_option, ...control_props }) {
  const stock_item_id = Form.useWatch('__production_stock_item_id', form);
  const planned_starts_on = Form.useWatch('planned_starts_on', form);
  const [options, set_options] = useState([]);
  const [loading, set_loading] = useState(false);
  const [load_failed, set_load_failed] = useState(false);

  useEffect(() => {
    let mounted = true;
    set_options([]);
    set_load_failed(false);
    if (!stock_item_id) {
      form.setFieldValue('bom_id', undefined);
      set_loading(false);
      return () => { mounted = false; };
    }
    set_loading(true);
    const effective_date = to_iso_date(planned_starts_on);
    fetch_all_items('/api/v1/production/boms?stock_item_id=' + encodeURIComponent(stock_item_id)
      + '&status=active&page=0&page_size=100').then((items) => {
      if (!mounted) return;
      const effective_items = items.filter((item) => {
        if (!effective_date) return true;
        return (!item.valid_from || item.valid_from <= effective_date)
          && (!item.valid_to || item.valid_to >= effective_date);
      });
      const next_options = effective_items.map((item) => ({
        value: item.bom_id,
        label: item.bom_code + ' · v' + item.version_number + ' — ' + item.product_item_name,
      }));
      set_options(next_options);
      const current_value = form.getFieldValue('bom_id');
      const current_is_valid = next_options.some((option) => String(option.value) === String(current_value));
      form.setFieldValue('bom_id', current_is_valid ? current_value : next_options[0]?.value);
    }).catch(() => {
      if (mounted) {
        set_options([]);
        set_load_failed(true);
        form.setFieldValue('bom_id', undefined);
      }
    }).finally(() => {
      if (mounted) set_loading(false);
    });
    return () => { mounted = false; };
  }, [form, planned_starts_on, stock_item_id]);

  const selected_value = selected_option?.value;
  const visible_options = selected_value === undefined || selected_value === null
    || options.some((option) => String(option.value) === String(selected_value))
    ? options
    : [selected_option, ...options];

  return <Select
    {...control_props}
    showSearch
    allowClear={false}
    optionFilterProp="label"
    options={visible_options}
    loading={loading}
    disabled={!stock_item_id || loading}
    placeholder={load_failed ? 'Không tải được BOM' : stock_item_id ? 'Chọn BOM phù hợp' : 'Chọn dòng kế hoạch trước'}
    style={{ width: '100%' }}
  />;
}

function clear_lookup_cache() {
  lookup_cache.clear();
}

export { LookupField, PlanLineField, ProductionBomField, ProductionPlanField, clear_lookup_cache, lookup_definitions };
