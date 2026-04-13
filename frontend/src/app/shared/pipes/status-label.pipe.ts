import { Pipe, PipeTransform } from '@angular/core';
import { BoatStatus } from '../services/boat.service';

@Pipe({ name: 'statusLabel', standalone: true, pure: true })
export class StatusLabelPipe implements PipeTransform {
  transform(status: BoatStatus): string {
    const map: Record<BoatStatus, string> = {
      UNDERWAY:    'UNDERWAY',
      IN_PORT:     'IN PORT',
      MAINTENANCE: 'MAINTENANCE'
    };
    return map[status] ?? status;
  }
}
