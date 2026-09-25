// Labeled text input with an error line below it.
export default function Input({ label, error, ...props }) {
  return (
    <label className="block text-sm">
      {label && <span className="text-slate-600 font-medium">{label}</span>}
      <input
        className={`mt-1 w-full border rounded-lg px-3 py-2 bg-white ${
          error ? 'border-error' : 'border-slate-300'
        }`}
        {...props}
      />
      {error && <span className="text-xs text-error">{error}</span>}
    </label>
  );
}
