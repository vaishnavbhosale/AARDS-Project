// One button for the whole app. variant: primary | secondary | danger.
export default function Button({
  children,
  variant = 'primary',
  loading = false,
  className = '',
  ...props
}) {
  const styles = {
    primary: 'bg-primary text-white hover:bg-primary-dark',
    secondary: 'bg-white text-slate-700 border hover:bg-slate-50',
    danger: 'bg-error text-white hover:opacity-90',
  };
  return (
    <button
      disabled={loading || props.disabled}
      className={`rounded-lg px-4 py-2 text-sm font-medium disabled:opacity-60 ${styles[variant]} ${className}`}
      {...props}
    >
      {loading ? 'Please wait...' : children}
    </button>
  );
}
