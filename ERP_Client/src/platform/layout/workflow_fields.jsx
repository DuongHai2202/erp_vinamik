import { useEffect, useState } from 'react';
import { Form, Select } from 'antd';
import { request_api } from '../common/api_client';

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
    endpoint: '/api/v1/inventory/materials?page=0&page_size=200&status=active&item_type=raw_material',
    map_item: (item) => ({ value: item.stock_item_id, label: item.item_code + ' — ' + item.item_name }),
  },
  finished_products: {
    endpoint: '/api/v1/inventory/materials?page=0&page_size=200&status=active&item_type=finished_product',
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

async function fetch_lookup(lookup_key, endpoint) {
  const cache_key = lookup_key + '|' + endpoint;
  const definition = lookup_definitions[lookup_key] || {};
  const cacheable = !definition.remote_search;
  if (cacheable && lookup_cache.has(cache_key)) return lookup_cache.get(cache_key);
  if (lookup_requests.has(cache_key)) return lookup_requests.get(cache_key);
  const request = request_api(endpoint).then((response) => {
    const options = response_items(response).map((item) => definition.map_item ? definition.map_item(item) : item).filter((item) => item?.value !== undefined);
    if (cacheable) lookup_cache.set(cache_key, options);
    return options;
  }).finally(() => lookup_requests.delete(cache_key));
  lookup_requests.set(cache_key, request);
  return request;
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

  useEffect(() => {
    let mounted = true;
    if (!field.lookup || !endpoint) {
      set_options(field.options || []);
      set_load_failed(false);
      return () => { mounted = false; };
    }
    set_loading(true);
    set_load_failed(false);
    fetch_lookup(field.lookup, endpoint).then((next_options) => { if (mounted) set_options(next_options); }).catch(() => { if (mounted) { set_options([]); set_load_failed(true); } }).finally(() => { if (mounted) set_loading(false); });
    return () => { mounted = false; };
  }, [endpoint, field.lookup, field.options]);

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
  return <Select {...field.select_props} {...control_props} showSearch allowClear={!field.required} optionFilterProp={is_remote ? undefined : 'label'} filterOption={is_remote ? false : undefined} onSearch={is_remote ? set_search_value : undefined} onBlur={is_remote ? handle_remote_blur : undefined} options={field.lookup ? visible_options : (field.options || [])} loading={loading} disabled={Boolean(field.lookup && !endpoint)} placeholder={load_failed ? 'Không tải được danh mục' : field.placeholder || 'Chọn dữ liệu'} style={{ width: '100%', ...(field.select_props?.style || {}) }} />;
}

function PlanLineField({ form, ...control_props }) {
  const plan_id = Form.useWatch('production_plan_id', form);
  const [options, set_options] = useState([]);
  const [loading, set_loading] = useState(false);
  useEffect(() => {
    let mounted = true;
    const previous_line_id = form.getFieldValue('production_plan_line_id');
    set_options([]);
    if (!plan_id) {
      if (previous_line_id !== undefined && previous_line_id !== null) form.setFieldValue('production_plan_line_id', undefined);
      set_loading(false);
      return () => { mounted = false; };
    }
    set_loading(true);
    request_api('/api/v1/production/plans/' + plan_id).then((response) => {
      if (!mounted) return;
      const lines = response?.data?.lines || [];
      const next_options = lines.map((line) => ({ value: line.production_plan_line_id, label: line.stock_item_code + ' — ' + line.stock_item_name + ' · ' + line.target_quantity + ' ' + line.unit_code }));
      set_options(next_options);
      // Preserve the existing line when it belongs to the selected plan. If
      // the user changes the plan, remove only the stale line after the new
      // plan has been loaded; this keeps edit forms populated correctly.
      if (previous_line_id !== undefined && previous_line_id !== null
        && !next_options.some((option) => String(option.value) === String(previous_line_id))) {
        form.setFieldValue('production_plan_line_id', undefined);
      }
    }).catch(() => { if (mounted) set_options([]); }).finally(() => { if (mounted) set_loading(false); });
    return () => { mounted = false; };
  }, [form, plan_id]);
  return <Select {...control_props} showSearch allowClear optionFilterProp="label" options={options} loading={loading} disabled={!plan_id} placeholder={plan_id ? 'Chọn dòng kế hoạch' : 'Chọn kế hoạch trước'} style={{ width: '100%' }} />;
}

function clear_lookup_cache() {
  lookup_cache.clear();
}

export { LookupField, PlanLineField, clear_lookup_cache, lookup_definitions };
