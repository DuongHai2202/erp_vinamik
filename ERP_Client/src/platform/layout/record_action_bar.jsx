import { Button, Dropdown, Tooltip } from 'antd';
import {
  CheckCircleOutlined,
  CheckOutlined,
  DeleteOutlined,
  FileSearchOutlined,
  FormOutlined,
  MoreOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  SendOutlined,
  StopOutlined,
  UndoOutlined,
} from '@ant-design/icons';

function action_icon(action) {
  if (action.icon) return action.icon;
  if (action.key === 'delete') return <DeleteOutlined />;
  if (['cancel', 'reject', 'deactivate', 'terminate', 'stop'].includes(action.key)) return <StopOutlined />;
  if (action.key === 'pause') return <PauseCircleOutlined />;
  if (['start', 'resume'].includes(action.key)) return <PlayCircleOutlined />;
  if (['approve', 'complete', 'activate'].includes(action.key)) return <CheckCircleOutlined />;
  if (action.key === 'post' || action.key === 'submit' || action.key === 'release' || action.key === 'calculate') return <SendOutlined />;
  if (action.key === 'lock') return <StopOutlined />;
  if (action.key === 'undo') return <UndoOutlined />;
  return <CheckOutlined />;
}

function is_danger_action(action) {
  return ['cancel', 'reject', 'delete', 'deactivate', 'terminate', 'stop'].includes(action.key);
}

function RecordActionBar({
  on_open,
  on_edit,
  can_edit = false,
  edit_status_allowed = true,
  edit_disabled_reason = "Chỉ chỉnh sửa bản nháp",
  workflow_actions = [],
}) {
  // Keep the first two workflow actions visible; labels are available through hover tooltips.
  const visible_actions = workflow_actions.length > 1 ? workflow_actions.slice(0, 2) : workflow_actions;
  const overflow_actions = workflow_actions.length > 2 ? workflow_actions.slice(2) : [];
  const overflow_map = Object.fromEntries(overflow_actions.map((action) => [action.key, action]));

  const run_action = (action, event) => {
    event?.preventDefault();
    event?.stopPropagation();
    if (action?.on_click && !action.loading && !action.disabled) action.on_click(action);
  };

  const menu_items = overflow_actions.map((action) => ({
    key: action.key,
    icon: action_icon(action),
    danger: is_danger_action(action),
    label: action.disabled ? (action.disabled_reason || action.label) : action.label,
    disabled: Boolean(action.loading || action.disabled),
  }));

  return (
    <div className="record_action_bar" role="group" aria-label="Thao tác bản ghi">
      <Tooltip title="Mở">
        <span className="record_action_tooltip_target">
          <Button
            htmlType="button"
            type="text"
            className="record_action_button record_action_primary record_action_icon"
            icon={<FileSearchOutlined />}
            onMouseDown={(event) => event.stopPropagation()}
            onClick={(event) => { event.preventDefault(); event.stopPropagation(); on_open?.(event); }}
            aria-label="Mở"
          />
        </span>
      </Tooltip>
      {can_edit && (
        <Tooltip title={edit_status_allowed ? "Chỉnh sửa" : edit_disabled_reason}>
          <span className="record_action_tooltip_target">
            <Button
              htmlType="button"
              type="text"
              className="record_action_button record_action_icon"
              icon={<FormOutlined />}
              disabled={!edit_status_allowed}
              onMouseDown={(event) => event.stopPropagation()}
              onClick={(event) => { event.preventDefault(); event.stopPropagation(); on_edit?.(event); }}
              aria-label="Chỉnh sửa"
            />
          </span>
        </Tooltip>
      )}
      {visible_actions.map((action) => (
        <Tooltip key={action.key} title={action.disabled ? (action.disabled_reason || action.label) : action.label}>
          <span className="record_action_tooltip_target">
            <Button
              type="text"
              htmlType="button"
              className={'record_action_button record_action_transition record_action_icon' + (is_danger_action(action) ? ' record_action_danger' : '')}
              icon={action_icon(action)}
              loading={Boolean(action.loading)}
              disabled={Boolean(action.disabled)}
              onMouseDown={(event) => event.stopPropagation()}
              onClick={(event) => run_action(action, event)}
              aria-label={action.label}
              aria-busy={Boolean(action.loading)}
            />
          </span>
        </Tooltip>
      ))}
      {overflow_actions.length > 0 && (
        <Dropdown
          trigger={['click']}
          menu={{
            items: menu_items,
            onClick: ({ key, domEvent }) => {
              domEvent?.stopPropagation();
              run_action(overflow_map[key], domEvent);
            },
          }}
        >
          <Tooltip title="Thao tác khác">
            <span className="record_action_tooltip_target">
              <Button
                htmlType="button"
                type="text"
                className="record_action_button record_action_icon record_action_overflow"
                icon={<MoreOutlined />}
                aria-label="Thao tác khác"
              />
            </span>
          </Tooltip>
        </Dropdown>
      )}
    </div>
  );
}

export default RecordActionBar;
