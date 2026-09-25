export default function SkeletonCard() {
  return (
    <div className="bg-white rounded-xl shadow p-4 space-y-2 animate-pulse">
      <div className="h-3 bg-slate-200 rounded w-1/2" />
      <div className="h-6 bg-slate-200 rounded w-3/4" />
    </div>
  );
}
