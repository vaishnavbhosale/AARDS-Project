// Colored message box. type: error | success | warning.
export default function Alert({ type = 'error', children }) {
  const styles = {
    error: 'bg-red-50 border-red-200 text-red-700',
    success: 'bg-green-50 border-green-200 text-green-700',
    warning: 'bg-orange-50 border-orange-200 text-orange-700',
  };
  return (
    <div className={`border rounded-lg px-4 py-2 text-sm ${styles[type]}`}>
      {children}
    </div>
  );
}
