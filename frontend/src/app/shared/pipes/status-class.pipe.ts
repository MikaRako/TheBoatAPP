import { Pipe, PipeTransform } from '@angular/core';
import { BoatStatus } from '../services/boat.service';

@Pipe({ name: 'statusClass', standalone: true, pure: true })
export class StatusClassPipe implements PipeTransform {
  transform(status: BoatStatus): string {
    const map: Record<BoatStatus, string> = {
      UNDERWAY:    'badge-active',
      IN_PORT:     'badge-port',
      MAINTENANCE: 'badge-maintenance'
    };
    return map[status] ?? '';
  }
}
