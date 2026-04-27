import React from 'react';
import type { TemplateNode } from '../../../types/template';

interface Props { node: TemplateNode }

export const NodeViewer: React.FC<Props> = ({ node }) => {
  switch (node.type) {
    case 'CONTAINER':
      return (
        <div className="card shadow-sm">
          <div className="card-header fw-semibold">{node.title}</div>
          <div className="card-body">
            <div className="row g-2">
              {node.children.map((child) => (
                <div key={child.id} className="col-12 col-sm-6 col-md-4">
                  <NodeViewer node={child} />
                </div>
              ))}
            </div>
          </div>
        </div>
      );

    case 'BLOCK':
      return (
        <div className="border rounded p-3 bg-light">
          <div className="text-muted small mb-1">{node.label}</div>
          <div className="fw-bold fs-5">{node.defaultValue ?? '—'}</div>
        </div>
      );

    case 'COUNTER':
      return (
        <div className="border rounded p-3 bg-light">
          <div className="text-muted small mb-1">{node.label}</div>
          <div className="fw-bold fs-5">
            {node.currentValue}
            {node.maxValue != null && <span className="text-muted fs-6"> / {node.maxValue}</span>}
          </div>
          {node.maxValue != null && (
            <div className="progress mt-2" style={{ height: 6 }}>
              <div
                className="progress-bar"
                style={{ width: `${(node.currentValue / node.maxValue) * 100}%` }}
              />
            </div>
          )}
        </div>
      );

    case 'TABLE':
      return (
        <div className="border rounded p-3">
          <div className="text-muted small mb-2">{node.label}</div>
          <div className="table-responsive">
            <table className="table table-bordered table-sm mb-0">
              <tbody>
                {Array.from({ length: node.rows }, (_, r) => (
                  <tr key={r}>
                    {Array.from({ length: node.columns }, (_, c) => (
                      <td key={c} className="p-2" style={{ minWidth: 60 }} />
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      );

    case 'TEXT_FIELD':
      return (
        <textarea
          className="form-control"
          rows={3}
          placeholder={node.placeholder}
          readOnly
        />
      );
  }
};