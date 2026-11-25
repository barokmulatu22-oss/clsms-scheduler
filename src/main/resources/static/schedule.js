// schedule.js - Schedule page: weekly timetable + new/edit/cancel booking form.
// The server decides what is allowed; this file only shows what the API returns.

const HOUR_PX = 60;          // must match the 60px lines in styles.css (.day-col)
let editingId = null;        // schedule ID being edited, or null for a new booking

// ---------- timetable ----------

// Gives each booking a lane so bookings that run at the same time sit side by side.
function assignLanes(items) {
  const laneEnds = [];
  items.slice().sort((a, b) => toMinutes(a.startTime) - toMinutes(b.startTime)).forEach(item => {
    let lane = laneEnds.findIndex(end => end <= toMinutes(item.startTime));
    if (lane === -1) lane = laneEnds.length;
    laneEnds[lane] = toMinutes(item.endTime);
    item.lane = lane;
  });
  return Math.max(laneEnds.length, 1);
}

function bookingBlock(item, firstHour, laneCount) {
  const top = (toMinutes(item.startTime) - firstHour * 60) * HOUR_PX / 60;
  const height = (toMinutes(item.endTime) - toMinutes(item.startTime)) * HOUR_PX / 60;
  const width = 100 / laneCount;
  const label = item.courseCode + ', ' + item.instructorName + ', ' + item.roomName
    + ', ' + dayName(item.day) + ' ' + timeRange(item.startTime, item.endTime);
  return el('button', {
    type: 'button',
    class: 'booking ' + item.roomType.toLowerCase(),
    style: 'top:' + top + 'px;height:' + (height - 1) + 'px;left:calc(' + (item.lane * width) + '% + 1px);width:calc(' + width + '% - 2px)',
    title: label,
    'aria-label': 'Edit booking ' + label,
    onclick: () => startEdit(item)
  }, el('strong', {}, item.courseCode), el('span', {}, item.instructorName), el('span', {}, item.roomName));
}

function drawTimetable(schedules) {
  // Monday to Friday always; Saturday/Sunday only when they have bookings
  const days = DAYS.filter((day, index) => index < 5 || schedules.some(s => s.day === day));

  let firstHour = 7, lastHour = 19;
  schedules.forEach(s => {
    firstHour = Math.min(firstHour, Math.floor(toMinutes(s.startTime) / 60));
    lastHour = Math.max(lastHour, Math.ceil(toMinutes(s.endTime) / 60));
  });
  const bodyHeight = (lastHour - firstHour) * HOUR_PX;

  const timeColumn = el('div', { class: 'time-col', style: 'height:' + bodyHeight + 'px' });
  for (let hour = firstHour; hour <= lastHour; hour++) {
    const text = String(hour).padStart(2, '0') + ':00';
    timeColumn.append(el('div', { class: 'time-label', style: 'top:' + (hour - firstHour) * HOUR_PX + 'px' }, text));
  }

  const columns = days.map(day => {
    const items = schedules.filter(s => s.day === day);
    const laneCount = assignLanes(items);
    return el('div', { class: 'day-col', style: 'height:' + bodyHeight + 'px' },
      items.map(item => bookingBlock(item, firstHour, laneCount)));
  });

  const head = el('div', { class: 'tt-head', style: '--days:' + days.length },
    el('div', {}), days.map(day => el('div', { class: 'tt-day' }, dayName(day))));
  const body = el('div', { class: 'tt-body', style: '--days:' + days.length }, timeColumn, columns);

  const parts = [head, body];
  if (schedules.length === 0) {
    parts.push(el('p', { class: 'empty', style: 'padding:0 12px' }, 'No bookings to show.'));
  }
  $('timetable').replaceChildren(...parts);
}

async function loadSchedules() {
  try {
    const schedules = await Api.schedules({
      room: $('filterRoom').value,
      instructor: $('filterInstructor').value,
      course: $('filterCourse').value
    });
    drawTimetable(schedules);
  } catch (err) {
    $('timetable').replaceChildren(el('p', { class: 'empty', style: 'padding:12px' }, 'The timetable could not be loaded.'));
    showErrors(err);
  }
}

// ---------- booking form ----------

function formData() {
  return {
    courseCode: $('fCourse').value,
    instructorId: $('fInstructor').value,
    roomId: $('fRoom').value,
    day: $('fDay').value,
    startTime: $('fStart').value,
    endTime: $('fEnd').value
  };
}

function resetForm() {
  editingId = null;
  $('bookingForm').reset();
  $('formTitle').textContent = 'New booking';
  $('saveBtn').textContent = 'Create booking';
  $('cancelBookingBtn').hidden = true;
  $('stopEditBtn').hidden = true;
}

function startEdit(item) {
  clearMessages();
  editingId = item.scheduleId;
  $('fCourse').value = item.courseCode;
  $('fInstructor').value = item.instructorId;
  $('fRoom').value = item.roomId;
  $('fDay').value = item.day;
  $('fStart').value = formatTime(item.startTime);
  $('fEnd').value = formatTime(item.endTime);
  $('formTitle').textContent = 'Edit booking ' + item.scheduleId;
  $('saveBtn').textContent = 'Save changes';
  $('cancelBookingBtn').hidden = false;
  $('stopEditBtn').hidden = false;
  $('bookingCard').scrollIntoView({ block: 'nearest' });
  $('fCourse').focus();
}

async function saveBooking(event) {
  event.preventDefault();
  clearMessages();
  await guarded($('saveBtn'), async () => {
    const wasEditing = editingId !== null;
    const saved = wasEditing
      ? await Api.updateSchedule(editingId, formData())
      : await Api.createSchedule(formData());
    showMessage('Booking ' + saved.scheduleId + (wasEditing ? ' updated: ' : ' created: ')
      + saved.courseCode + ' in ' + saved.roomName + ' on ' + dayName(saved.day)
      + ' ' + timeRange(saved.startTime, saved.endTime) + '.');
    resetForm();
    await loadSchedules();
  });
  // On failure the form keeps what the user typed.
}

async function cancelBooking() {
  if (editingId === null) return;
  if (!confirm('Cancel booking ' + editingId + '? This cannot be undone.')) return;
  clearMessages();
  await guarded($('cancelBookingBtn'), async () => {
    const result = await Api.cancelSchedule(editingId);
    showMessage(result.message);
    resetForm();
    await loadSchedules();
  });
}

// ---------- start-up ----------

async function init() {
  renderSidebar('index.html');
  $('fDay').replaceChildren(...DAYS.map(day => el('option', { value: day }, dayName(day))));

  const roomText = r => r.roomId + ' - ' + r.name + ' (' + r.capacity + ' seats)' + (r.active ? '' : ' [inactive]');
  const courseText = c => c.courseCode + ' - ' + c.title;
  try {
    const [rooms, courses, instructors] = await Promise.all([Api.rooms(), Api.courses(), Api.instructors()]);
    fillSelect($('filterRoom'), rooms, r => r.roomId, roomText, 'All rooms');
    fillSelect($('filterInstructor'), instructors, i => i.instructorId, i => i.fullName, 'All instructors');
    fillSelect($('filterCourse'), courses, c => c.courseCode, courseText, 'All courses');
    fillSelect($('fRoom'), rooms, r => r.roomId, roomText, 'Choose a room');
    fillSelect($('fInstructor'), instructors, i => i.instructorId, i => i.fullName, 'Choose an instructor');
    fillSelect($('fCourse'), courses, c => c.courseCode, courseText, 'Choose a course');
  } catch (err) {
    showErrors(err);
  }

  ['filterRoom', 'filterInstructor', 'filterCourse'].forEach(id => $(id).addEventListener('change', loadSchedules));
  $('clearFilters').addEventListener('click', () => {
    ['filterRoom', 'filterInstructor', 'filterCourse'].forEach(id => { $(id).value = ''; });
    loadSchedules();
  });
  $('bookingForm').addEventListener('submit', saveBooking);
  $('cancelBookingBtn').addEventListener('click', cancelBooking);
  $('stopEditBtn').addEventListener('click', () => { clearMessages(); resetForm(); });

  await loadSchedules();
}

init();
