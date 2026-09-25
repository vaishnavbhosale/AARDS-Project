// Centered loading spinner.
export default function Loader() {
  return (
    <div className="flex items-center justify-center py-10">
      <div className="h-8 w-8 rounded-full border-2 border-slate-300 border-t-primary animate-spin" />
    </div>
  );
}
