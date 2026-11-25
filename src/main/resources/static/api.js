// api.js - the ONLY file that calls fetch. One function per endpoint.
// Every failure is thrown as { status, errors: [...] } so pages can show it the same way.

const Api = (() => {
  async function request(method, url, body) {
    let response;
    try {
      response = await fetch(url, {
        method: method,
        headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body)
      });
    } catch (networkError) {
      throw { status: 0, errors: ['Cannot reach the server. Check that the app is running, then try again.'] };
    }

    let data = null;
    try {
      data = await response.json();
    } catch (notJson) {
      data = null; // empty or non-JSON body
    }

    if (!response.ok) {
      const errors = data && Array.isArray(data.errors) && data.errors.length > 0
        ? data.errors
        : ['The server returned an error (status ' + response.status + ').'];
      throw { status: response.status, errors: errors };
    }
    return data;
  }

  // Builds "?a=1&b=2" and skips blank values.
  function query(params) {
    const search = new URLSearchParams();
    Object.keys(params || {}).forEach(key => {
      const value = params[key];
      if (value !== undefined && value !== null && String(value).trim() !== '') {
        search.set(key, value);
      }
    });
    const text = search.toString();
    return text ? '?' + text : '';
  }

  const enc = encodeURIComponent;

  return {
    // rooms
    rooms: (search) => request('GET', '/api/rooms' + query({ search: search })),
    createRoom: (room) => request('POST', '/api/rooms', room),
    updateRoom: (id, room) => request('PUT', '/api/rooms/' + enc(id), room),
    deleteRoom: (id) => request('DELETE', '/api/rooms/' + enc(id)),

    // courses
    courses: (search) => request('GET', '/api/courses' + query({ search: search })),
    createCourse: (course) => request('POST', '/api/courses', course),
    updateCourse: (code, course) => request('PUT', '/api/courses/' + enc(code), course),
    deleteCourse: (code) => request('DELETE', '/api/courses/' + enc(code)),

    // instructors
    instructors: (search) => request('GET', '/api/instructors' + query({ search: search })),
    createInstructor: (person) => request('POST', '/api/instructors', person),
    updateInstructor: (id, person) => request('PUT', '/api/instructors/' + enc(id), person),
    deleteInstructor: (id) => request('DELETE', '/api/instructors/' + enc(id)),

    // schedules (filters: room, course, instructor, day, time)
    schedules: (filters) => request('GET', '/api/schedules' + query(filters)),
    createSchedule: (booking) => request('POST', '/api/schedules', booking),
    updateSchedule: (id, booking) => request('PUT', '/api/schedules/' + enc(id), booking),
    cancelSchedule: (id) => request('DELETE', '/api/schedules/' + enc(id)),

    // availability (params: day, start, end, minCapacity, type)
    availability: (params) => request('GET', '/api/availability' + query(params)),

    // reports
    dailyReport: (day) => request('GET', '/api/reports/daily' + query({ day: day })),
    roomReport: (id) => request('GET', '/api/reports/room/' + enc(id)),
    instructorReport: (id) => request('GET', '/api/reports/instructor/' + enc(id)),
    summary: () => request('GET', '/api/reports/summary')
  };
})();
