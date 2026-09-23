import { Input } from 'antd';

const money_keys = new Set([
  'amount',
  'base_salary',
  'base_salary_snapshot',
  'reward_amount',
  'discipline_amount',
  'tax_amount',
  'insurance_amount',
  'overtime_amount',
  'total_net_amount',
  'material_cost',
  'direct_labor_cost',
  'overhead_cost',
  'adjustment_amount',
  'total_cost',
  'unit_cost',
  'proposed_price',
  'salary_amount',
  'gross_amount',
  'net_amount',
]);

const date_keys = new Set([
  'date_of_birth',
  'hired_on',
  'terminated_on',
  'effective_from',
  'effective_to',
  'starts_on',
  'ends_on',
  'planned_on',
  'required_on',
  'effective_on',
  'valid_from',
  'valid_to',
  'manufactured_on',
  'expires_on',
]);

function format_number_vn(value, maximum_fraction_digits = 2) {
  const number = Number(value);
  if (!Number.isFinite(number)) return String(value ?? '');
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: maximum_fraction_digits }).format(number);
}

function format_vnd(value) {
  if (value === null || value === undefined || value === '') return '—';
  const number = Number(value);
  return Number.isFinite(number) ? `${format_number_vn(number)}đ` : String(value);
}

function format_date_vn(value) {
  if (value === null || value === undefined || value === '') return '—';
  const text = String(value);
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (match) return `${match[3]}/${match[2]}/${match[1]}`;
  const already_formatted = text.match(/^(\d{2})\/(\d{2})\/(\d{4})/);
  return already_formatted ? already_formatted[0] : text;
}

function format_datetime_vn(value) {
  if (value === null || value === undefined || value === '') return '—';
  const text = String(value);
  const date = format_date_vn(text);
  const time_match = text.match(/[T ](\d{2}:\d{2})(?::(\d{2}))?/);
  return time_match ? `${date} ${time_match[1]}${time_match[2] ? `:${time_match[2]}` : ''}` : date;
}

function to_iso_date(value) {
  if (value === null || value === undefined || value === '') return null;
  const text = String(value).trim();
  const vietnamese = text.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
  if (vietnamese) return `${vietnamese[3]}-${vietnamese[2]}-${vietnamese[1]}`;
  return text.match(/^\d{4}-\d{2}-\d{2}$/) ? text : text;
}

function format_field_value(value, field_key) {
  if (value === null || value === undefined || value === '') return '—';
  if (money_keys.has(field_key)) return format_vnd(value);
  if (date_keys.has(field_key)) return format_date_vn(value);
  if (typeof value === 'string' && /^(\d{4})-(\d{2})-(\d{2})(?:T|$)/.test(value)) return format_datetime_vn(value);
  return value;
}

/**
 * Form value remains a plain dd/MM/yyyy string at the UI boundary. Submitters
 * convert it with to_iso_date so API/database values stay yyyy-MM-dd.
 */
function VietnameseDateInput({ value, onChange, placeholder = 'dd/MM/yyyy', ...props }) {
  const display_value = value ? format_date_vn(value) : '';
  return <Input {...props} value={display_value} placeholder={placeholder} onChange={onChange} />;
}

export { date_keys, format_date_vn, format_datetime_vn, format_field_value, format_number_vn, format_vnd, money_keys, to_iso_date, VietnameseDateInput };
