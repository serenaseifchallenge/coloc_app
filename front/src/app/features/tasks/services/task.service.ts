import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTaskRequest, Task, TaskQuery } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/tasks';
  private readonly taskNamesUrl = '/api/task-names';

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
    return this.http.get<string[]>(this.taskNamesUrl);
  }

  addTaskName(name: string): Observable<{ name: string }> {
    return this.http.post<{ name: string }>(this.taskNamesUrl, { name });
  }

  deleteTaskName(name: string): Observable<void> {
    return this.http.delete<void>(`${this.taskNamesUrl}/${encodeURIComponent(name)}`);
  }
}