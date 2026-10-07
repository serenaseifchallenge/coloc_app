import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Task, TaskQuery } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/tasks';


    getTasks(query: TaskQuery): Observable<Task[]> {
    return this.http.get<Task[]>(this.apiUrl, {
      params: { done: query.done, assignedToMe: query.assignedToMe },
    });
  }
}