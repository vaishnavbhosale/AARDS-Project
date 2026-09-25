// White content box with soft shadow.
export default function Card({ children }) {
  return <div className="bg-white rounded-xl shadow p-4 md:p-5">{children}</div>;
}
