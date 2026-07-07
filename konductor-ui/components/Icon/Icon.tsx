export type IconName =
  | 'hub'
  | 'sensors'
  | 'add'
  | 'webhook'
  | 'bolt'
  | 'visibility'
  | 'edit'
  | 'delete'
  | 'check'
  | 'close'
  | 'power_settings_new'
  | 'tune'
  | 'arrow_back'
  | 'description'
  | 'check_circle'
  | 'inbox';

interface Props {
  name: IconName;
  size?: number;
}

export default function Icon({ name, size = 18 }: Props) {
  return (
    <span className="material-symbols-rounded" style={{ fontSize: size }} aria-hidden="true">
      {name}
    </span>
  );
}
