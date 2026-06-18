import React from "react";

export function AdminPageHeader({ title, description }: { title: string; description: string }) {
  return (
    <div className="admin-page-header">
      <div>
        <h1 data-testid="admin-page-title">{title}</h1>
        <p>{description}</p>
      </div>
    </div>
  );
}
