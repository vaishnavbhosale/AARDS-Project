// Labeled dropdown. options = [{ value, label }].
export default function Select({ label, options = [], ...props }) {
  return (
    <label className="block text-sm">
      {label && <span className="text-slate-600 font-medium">{label}</span>}
      <select
        className="mt-1 w-full border border-slate-300 rounded-lg px-3 py-2 bg-white"
        {...props}
      >
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </label>
  );
}
