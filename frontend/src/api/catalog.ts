import { collection } from './collections';
import { http } from './http';
import type { Availability, Certification, Consumable, Created, Instrument, Period, Permit, Person, Uuid, Vehicle } from './types';

export const certificationsApi = {
  ...collection<Certification>('/v1/certifications'),
  register: (name: string) => http.post<Created>('/v1/certifications', { name }),
};

export const peopleApi = {
  ...collection<Person>('/v1/people'),
  register: (name: string, certifications: Uuid[], availability: Availability) =>
    http.post<Created>('/v1/people', { name, certifications, availability }),
  certify: (id: Uuid, certificationId: Uuid) => http.put(`/v1/people/${id}/certifications/${certificationId}`),
  changeAvailability: (id: Uuid, availability: Availability) => http.put(`/v1/people/${id}/availability`, { availability }),
};

export const vehiclesApi = {
  ...collection<Vehicle>('/v1/vehicles'),
  register: (capacity: number, availability: Availability) => http.post<Created>('/v1/vehicles', { capacity, availability }),
  changeAvailability: (id: Uuid, availability: Availability) => http.put(`/v1/vehicles/${id}/availability`, { availability }),
};

export const instrumentsApi = {
  ...collection<Instrument>('/v1/instruments'),
  register: (kind: string, availability: Availability) => http.post<Created>('/v1/instruments', { kind, availability }),
  changeAvailability: (id: Uuid, availability: Availability) => http.put(`/v1/instruments/${id}/availability`, { availability }),
};

export const consumablesApi = {
  ...collection<Consumable>('/v1/consumables'),
  register: (name: string, stock: number) => http.post<Created>('/v1/consumables', { name, stock }),
  changeStock: (id: Uuid, stock: number) => http.put(`/v1/consumables/${id}/stock`, { stock }),
};

export const permitsApi = {
  ...collection<Permit>('/v1/permits'),
  register: (kind: string, zone: string, validity: Period) => http.post<Created>('/v1/permits', { kind, zone, validity }),
};
