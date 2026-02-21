import React from 'react';
import styles from './DataTable.module.css';

/**
 * Simple table for list UIs. columns: [ { key, label, render?(value, row) } ]
 * Optional sortKey/sortDir for future sortable headers.
 */
export default function DataTable({ columns, data, emptyMessage = 'No data', sortKey, sortDir, onSort }) {
  if (!data || data.length === 0) {
    return <p className={styles.empty}>{emptyMessage}</p>;
  }
  return (
    <div className={styles.wrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            {columns.map(({ key, label, sortable }) => (
              <th key={key} className={styles.th}>
                {onSort && sortable !== false ? (
                  <button type="button" className={styles.sortBtn} onClick={() => onSort(key)}>
                    {label} {sortKey === key ? (sortDir === 'asc' ? ' ↑' : ' ↓') : ''}
                  </button>
                ) : (
                  label
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, i) => (
            <tr key={row.id || i}>
              {columns.map(({ key, render }) => (
                <td key={key} className={styles.td}>
                  {render ? render(row[key], row) : row[key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
