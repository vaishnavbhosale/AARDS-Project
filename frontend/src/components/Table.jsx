import { Children, cloneElement, isValidElement } from 'react';

// Simple table. headers = strings, children = <tr> rows.
// stickyCols={n} freezes the first n columns on horizontal scroll.
// (First frozen col is w-10 wide, so the second starts at left-10.)
export default function Table({ headers, children, stickyCols = 0 }) {
  function frozen(index, extra = '') {
    if (index >= stickyCols) return extra;
    const offset = index === 0 ? 'left-0' : 'left-10';
    return `${extra} sticky ${offset} bg-white z-10 border-r border-slate-200`;
  }

  function freezeRow(row) {
    if (!isValidElement(row) || stickyCols <= 0) return row;
    return cloneElement(row, {
      children: Children.map(row.props.children, (cell, i) => {
        if (!isValidElement(cell) || i >= stickyCols) return cell;
        const width = i === 0 ? 'w-10' : 'min-w-40';
        return cloneElement(cell, {
          className: `${cell.props.className || ''} ${width} ${frozen(i)}`,
        });
      }),
    });
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-slate-500 border-b">
            {headers.map((h, i) => (
              <th key={h} className={`py-2 pr-4 font-medium whitespace-nowrap ${frozen(i)}`}>
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{Children.map(children, freezeRow)}</tbody>
      </table>
    </div>
  );
}
