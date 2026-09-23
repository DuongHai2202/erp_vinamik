import { memo, useEffect, useRef, useState } from 'react';
import { Input } from 'antd';

const DebouncedSearchInput = memo(function DebouncedSearchInput({
  value = '',
  on_commit,
  delay = 350,
  ...props
}) {
  const [draft, set_draft] = useState(value || '');
  const timer_ref = useRef(null);
  const commit_ref = useRef(on_commit);

  useEffect(() => {
    commit_ref.current = on_commit;
  }, [on_commit]);

  useEffect(() => {
    set_draft(value || '');
  }, [value]);

  useEffect(() => () => {
    if (timer_ref.current) window.clearTimeout(timer_ref.current);
  }, []);

  const commit = (next_value) => {
    if (timer_ref.current) window.clearTimeout(timer_ref.current);
    commit_ref.current?.(next_value);
  };

  const on_change = (event) => {
    const next_value = event.target.value;
    set_draft(next_value);
    if (timer_ref.current) window.clearTimeout(timer_ref.current);
    timer_ref.current = window.setTimeout(() => {
      commit_ref.current?.(next_value);
      timer_ref.current = null;
    }, delay);
  };

  return (
    <Input.Search
      {...props}
      allowClear
      value={draft}
      onChange={on_change}
      onSearch={(next_value) => commit(next_value)}
    />
  );
});

export default DebouncedSearchInput;
