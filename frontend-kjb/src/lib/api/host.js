import { apiRequest } from "./client";

export function getHostReservationSummary() {
  return apiRequest("/api/host/reservations");
}