export interface Option {
  value: string;
  label: string;
}

interface CheckListProps {
  options: Option[];
  selected: string[];
  onChange: (selected: string[]) => void;
}

export function CheckList({ options, selected, onChange }: CheckListProps) {
  const toggle = (value: string) =>
    onChange(selected.includes(value) ? selected.filter((current) => current !== value) : [...selected, value]);

  if (options.length === 0) {
    return <span className="muted">No hay opciones</span>;
  }
  return (
    <div className="checklist">
      {options.map((option) => (
        <label key={option.value}>
          <input type="checkbox" checked={selected.includes(option.value)} onChange={() => toggle(option.value)} />
          {option.label}
        </label>
      ))}
    </div>
  );
}
