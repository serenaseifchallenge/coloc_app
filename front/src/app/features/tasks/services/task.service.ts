import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTaskRequest, Task, TaskQuery } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/tasks';

  getTasks(query: TaskQuery): Observable<Task[]> {
    return this.http.get<Task[]>(this.apiUrl, {
      params: { done: query.done, assignedToMe: query.assignedToMe },
    });
  }

  createTask(request: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(this.apiUrl, request);
  }

  completeTask(taskId: number): Observable<Task> {
    return this.http.patch<Task>(`${this.apiUrl}/${taskId}/complete`, null);
  }

  reopenTask(taskId: number): Observable<Task> {
    return this.http.patch<Task>(`${this.apiUrl}/${taskId}/reopen`, null);
  }

  deleteTask(taskId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${taskId}`);
  }

  getTaskNames(): Observable<string[]> {
    return this.http.get<string[]>('/api/task-names');
  }

  deleteTaskName(name: string): Observable<void> {
    return this.http.delete<void>(`/api/task-names/${encodeURIComponent(name)}`);
  }
}