import { useState } from 'react';
import { Icon } from './Icon';

interface TagInputProps {
  label: string;
  values: string[];
  onChange: (values: string[]) => void;
  placeholder: string;
  required?: boolean;
}

export function TagInput({ label, values, onChange, placeholder, required = false }: TagInputProps) {
  const [draft, setDraft] = useState('');

  const commit = () => {
    const value = draft.trim();
    if (value && !values.includes(value)) {
      onChange([...values, value]);
    }
    setDraft('');
  };

  return (
    <div className="tags">
      {values.map((value) => (
        <span key={value} className="tag">
          {value}
          <button type="button" aria-label={`Quitar ${value}`} onClick={() => onChange(values.filter((current) => current !== value))}>
            <Icon name="close" size={14} />
          </button>
        </span>
      ))}
      <input
        aria-label={label}
        value={draft}
        placeholder={values.length > 0 ? 'Agregar otro y presionar Enter' : placeholder}
        required={required && values.length === 0}
        onChange={(event) => setDraft(event.target.value)}
        onBlur={commit}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault();
            commit();
          } else if (event.key === 'Backspace' && draft === '' && values.length > 0) {
            onChange(values.slice(0, -1));
          }
        }}
      />
    </div>
  );
}
