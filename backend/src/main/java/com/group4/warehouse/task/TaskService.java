package com.group4.warehouse.task;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Text;

/** Rules for tasks: trimming input, 404 when the task does not exist. */
@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public List<Task> list(String q) {
        return repository.findAll(Text.trimToNull(q));
    }

    public Task get(String id) {
        return repository.findById(Text.trim(id)).orElseThrow(() -> notFound(id));
    }

    public Task create(CreateTaskRequest request) {
        String id = repository.insert(Text.trimToNull(request.id()), Text.trim(request.name()),
                Text.trim(request.staffId()), Text.trimToNull(request.status()));
        return get(id);
    }

    public Task update(String id, UpdateTaskRequest request) {
        if (request.name() == null && request.staffId() == null && request.status() == null) {
            throw ApiException.badRequest("Nothing to update: send name, staffId and/or status.");
        }
        if (!repository.update(Text.trim(id), Text.trimToNull(request.name()),
                Text.trimToNull(request.staffId()), Text.trimToNull(request.status()))) {
            throw notFound(id);
        }
        return get(id);
    }

    public void delete(String id) {
        if (!repository.delete(Text.trim(id))) {
            throw notFound(id);
        }
    }

    private static ApiException notFound(String id) {
        return ApiException.notFound("Task " + id + " was not found.");
    }
}
