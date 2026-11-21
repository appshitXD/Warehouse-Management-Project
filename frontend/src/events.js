export function bindEvents({
  $,
  $$,
  ctx,
  forms,
  stock,
  config,
  services,
  controller,
  env,
  storage,
}) {
  $('#operation-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (ctx.submitting || ctx.pendingMutation) return;
    forms.clearErrors();
    $('#form-error').textContent = '';
    $$('.line-error').forEach((el) => (el.textContent = ''));
    const form = $('#operation-form');
    if (!form.checkValidity()) {
      const invalid = $$('input,select', form).find((el) => !el.validity.valid),
        row = invalid?.closest('.line-row');
      if (row) {
        const error = new Error(
          'Please check the field ' +
            invalid.closest('.field').querySelector('span').textContent +
            '.',
        );
        error.lineIndex = $$('.line-row').indexOf(row);
        forms.showFormError(error);
      } else forms.showFormError(new Error('Complete all required fields.'));
      form.reportValidity();
      return;
    }
    try {
      const payload = forms.collectPayload();
      stock.validatePayload(ctx.operation, payload);
      forms.setSaving(true);
      if (config.mode === 'http') {
        await services.saveOperation(payload);
        $('#form-dialog').close();
        await controller.afterWrite(
          ctx.operation === 'count'
            ? payload.status && payload.status !== 'Completed'
              ? 'Stock count saved. Stock changes when it is Completed.'
              : 'Stock count recorded and stock adjusted.'
            : ctx.operation.startsWith('delete-')
              ? 'Record deleted.'
              : 'Saved successfully.',
        );
      } else {
        stock.applyLocal(ctx.operation, payload);
        $('#form-dialog').close();
        controller.render();
        controller.notify(
          ctx.operation === 'count'
            ? 'Stock count recorded and stock adjusted.'
            : ctx.operation.startsWith('delete-')
              ? 'Record deleted.'
              : 'Saved successfully.',
        );
      }
    } catch (error) {
      if (error.uncertain) {
        $('#form-dialog').close();
        controller.render();
        controller.notify(
          'The result is unconfirmed. Retry the previous request to avoid duplicate records.',
        );
      } else forms.showFormError(error);
    } finally {
      forms.setSaving(false);
    }
  });
  $('#close-dialog').onclick = $('#cancel-dialog').onclick = () => {
    if (!ctx.submitting) $('#form-dialog').close();
  };
  $('#form-dialog').addEventListener('cancel', (e) => {
    if (ctx.submitting) e.preventDefault();
  });
  env.document.addEventListener('click', (e) => {
    const action = e.target.closest('[data-operation]');
    if (action) {
      forms.openOperation(action.dataset.operation, action.dataset.edit || '');
      // An Edit/Delete button inside a selectable row must not also select the row.
      return;
    }
    if (e.target.closest('#add-staff-phone')) {
      forms.clearErrors();
      $('#staff-phone-list').insertAdjacentHTML('beforeend', forms.phoneForm());
      forms.describe();
      $$('[name="phones"]').at(-1)?.focus();
    }
    if (e.target.closest('.remove-staff-phone')) {
      forms.clearErrors();
      e.target.closest('.phone-row').remove();
      forms.describe();
      $('#add-staff-phone').focus();
    }
    const detail = e.target.closest('[data-detail]');
    if (detail) {
      ctx.filters.detail = detail.dataset.detail;
      controller.render();
    }
    const issue = e.target.closest('[data-issue]');
    if (issue) {
      ctx.filters.detail = issue.dataset.issue;
      controller.render();
      $('#issue-detail-heading')?.focus();
    }
    const directoryRetry = e.target.closest('[data-retry-directory]');
    if (directoryRetry)
      void services.loadDirectory(directoryRetry.dataset.retryDirectory, {
        retry: true,
      });
    if (e.target.closest('#add-line')) {
      $('#form-lines').insertAdjacentHTML('beforeend', forms.lineForm());
      forms.syncLine($$('.line-row').at(-1));
      forms.describe();
    }
    if (e.target.closest('.remove-line')) {
      if ($$('.line-row').length > 1) e.target.closest('.line-row').remove();
      else controller.notify('A document requires at least one line.');
      forms.clearErrors();
      forms.describe();
    }
    const errorLink = e.target.closest('#form-error-summary a');
    if (errorLink) {
      e.preventDefault();
      env.document
        .getElementById(errorLink.getAttribute('href').slice(1))
        ?.focus();
    }
    if (e.target.closest('#export-stock')) controller.exportStock();
    if (e.target.closest('#refresh-data') || e.target.closest('#retry-data'))
      void controller.refreshData();
    if (e.target.closest('#retry-pending')) void controller.retryPending();
    if (e.target.closest('#retry-reports')) void services.loadReports();
    if (e.target.closest('#retry-locations')) void services.loadLocations();
    const reportRetry = e.target.closest('[data-retry-operation-report]');
    if (reportRetry)
      void services.loadOperationReport(
        reportRetry.dataset.retryOperationReport,
        true,
      );
  });
  env.document.addEventListener('input', (e) => {
    if (e.target.id === 'search') {
      const previousQuery = ctx.filters.search.trim();
      ctx.filters.search = e.target.value;
      if (
        config.mode === 'http' &&
        ['customers', 'issues'].includes(ctx.route)
      ) {
        if (previousQuery === ctx.filters.search.trim()) return;
        ctx.filters.detail = '';
        const key = ctx.route;
        void services.loadDirectory(key, { debounce: true });
      } else controller.render();
    }
  });
  env.document.addEventListener('change', async (e) => {
    if (
      ['zone', 'aisle', 'bin'].includes(ctx.operation) &&
      ['warehouseId', 'zone'].includes(e.target.name) &&
      e.target.closest('#operation-form')
    )
      forms.syncLocation();
    if (
      ['productId', 'binId'].includes(e.target.name) &&
      e.target.closest('.line-row')
    )
      forms.syncLine(e.target.closest('.line-row'));
    if (
      ctx.operation === 'movement' &&
      ['productId', 'fromBinId', 'toBinId'].includes(e.target.name)
    )
      forms.syncMovement();
    if (e.target.id === 'customer-filter') {
      ctx.filters.customer = e.target.value;
      ctx.filters.detail = '';
      controller.render();
    }
    if (e.target.id === 'warehouse-filter') {
      ctx.filters.warehouse = e.target.value;
      controller.render();
    }
    if (e.target.id === 'product-filter') {
      ctx.filters.product = e.target.value;
      if (config.mode === 'http') void services.loadReports();
      else controller.render();
    }
    if (e.target.id === 'reports-as-of') {
      if (e.target.value && e.target.validity.valid)
        ctx.reportsAsOf = e.target.value;
      controller.render();
      $('#reports-as-of')?.focus();
    }
    if (e.target.id === 'inventory-product-filter') {
      ctx.filters.product = e.target.value;
      if (config.mode === 'http') void services.loadLocations();
      else controller.render();
    }
    // HTTP mode: changing a task's status saves it straight away (PATCH /tasks/{id}).
    if (config.mode === 'http' && e.target.dataset.task) {
      const id = e.target.dataset.task,
        status = e.target.value;
      if (
        ctx.refreshing ||
        ctx.stale ||
        ctx.pendingMutation ||
        ctx.submitting
      ) {
        controller.render();
        controller.notify(
          'Refresh the data or confirm the pending request first.',
        );
        return;
      }
      e.target.disabled = true;
      ctx.submitting = true;
      try {
        await services.sendMutation(
          '/tasks/' + encodeURIComponent(id),
          { status },
          'PATCH',
        );
        ctx.submitting = false;
        await controller.afterWrite('Task updated.');
      } catch (error) {
        controller.notify(error.message);
      } finally {
        ctx.submitting = false;
        controller.render();
      }
      return;
    }
    if (config.mode === 'demo' && e.target.dataset.task) {
      const id = e.target.dataset.task,
        status = e.target.value;
      e.target.disabled = true;
      if (
        ctx.refreshing ||
        ctx.stale ||
        ctx.pendingMutation ||
        ctx.submitting
      ) {
        controller.render();
        controller.notify(
          'Refresh the data or confirm the pending request first.',
        );
        return;
      }
      ctx.submitting = true;
      try {
        const next = structuredClone(ctx.state);
        next.tasks.find((t) => t.id === id).status = status;
        storage.persist(next);
        controller.notify('Task updated.');
      } catch (error) {
        controller.notify(error.message);
      } finally {
        ctx.submitting = false;
        controller.render();
      }
    }
  });
  env.window.addEventListener('hashchange', () => {
    controller.resetRouteFilters();
    controller.render();
    env.window.scrollTo(0, 0);
    $('#main').focus({ preventScroll: true });
  });
  env.window.addEventListener('storage', async (e) => {
    if (e.key === storage.key && config.mode === 'demo') {
      await controller.refreshData();
      if (!ctx.refreshError)
        controller.notify('Local data was updated in another tab.');
    }
  });
}
