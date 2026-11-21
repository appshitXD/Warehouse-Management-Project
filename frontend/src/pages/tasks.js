import { canWrite, httpTaskLabels, taskLabels } from '../config.js';
import { esc } from '../utils/format.js';
export function createTasks({ ctx, ui, selectors, config }) {
  const labels = config.mode === 'http' ? httpTaskLabels : taskLabels;
  const statusSelect = (task) =>
    canWrite(config, 'task')
      ? `<select aria-label="Status ${esc(task.name)}" data-task="${esc(task.id)}">${Object.entries(
          labels,
        )
          .map(
            ([value, label]) =>
              `<option value="${value}" ${task.status === value ? 'selected' : ''}>${label}</option>`,
          )
          .join('')}</select>`
      : '';
  function tasks() {
    if (!ctx.state.tasks.length)
      return (
        ui.heading(
          'Tasks',
          'Assign staff and track task progress.',
          ui.actionButton('task', 'Add task'),
        ) +
        ui.emptyState(
          'No tasks yet.',
          'Create a task and assign a staff member.',
        )
      );
    return (
      ui.heading(
        'Tasks',
        'Assign staff and track task progress.',
        ui.actionButton('task', 'Add task'),
      ) +
      `<div class="task-grid">${ctx.state.tasks
        .map(
          (t) =>
            `<article class="task-card">${ui.badge(labels[t.status], ['done', 'Completed'].includes(t.status) ? 'green' : ['doing', 'In Progress'].includes(t.status) ? 'amber' : 'gray')}<h3>${esc(t.name)}</h3><p>${esc(t.id)}</p><div class="row-between"><span class="muted">Assigned to</span><strong class="task-assignee">${esc(selectors.staffName(t.staffId))}</strong></div>${statusSelect(t)}</article>`,
        )
        .join('')}</div>`
    );
  }
  return { tasks };
}
