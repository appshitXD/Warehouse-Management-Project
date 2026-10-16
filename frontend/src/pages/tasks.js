import { taskLabels } from '../config.js';
import { esc } from '../utils/format.js';
export function createTasks({ ctx, ui, selectors }) {
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
            `<article class="task-card">${ui.badge(taskLabels[t.status], t.status === 'done' ? 'green' : t.status === 'doing' ? 'amber' : 'gray')}<h3>${esc(t.name)}</h3><p>${esc(t.id)}</p><div class="row-between"><span class="muted">Assigned to</span><strong class="task-assignee">${esc(selectors.staffName(t.staffId))}</strong></div><select aria-label="Status ${esc(t.name)}" data-task="${esc(t.id)}">${Object.entries(
              taskLabels,
            )
              .map(
                ([v, l]) =>
                  `<option value="${v}" ${t.status === v ? 'selected' : ''}>${l}</option>`,
              )
              .join('')}</select></article>`,
        )
        .join('')}</div>`
    );
  }
  return { tasks };
}
