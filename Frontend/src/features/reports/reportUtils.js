export const toUtcStartOfDay = (date) => {
  return date ? `${date}T00:00:00.000Z` : undefined;
};

export const toUtcEndOfDay = (date) => {
  return date ? `${date}T23:59:59.999Z` : undefined;
};

export const buildPeriodParams = ({ from, to }) => {
  const params = {};

  if (from) params.from = toUtcStartOfDay(from);
  if (to) params.to = toUtcEndOfDay(to);

  return params;
};