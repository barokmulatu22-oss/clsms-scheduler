// reports.js - Reports page: summary numbers plus daily, room and instructor timetables.

let roomChoices = [];
let instructorChoices = [];

// ---------- summary ----------

const STAT_LABELS = [
  ['totalRooms', 'Rooms'],
  ['activeRooms', 'Active rooms'],
  ['totalClassrooms', 'Classrooms'],
  ['totalLaboratories', 'Laboratories'],
  ['totalCourses', 'Courses'],
  ['totalInstructors', 'Instructors'],
  ['totalSchedules', 'Bookings']
];

async function loadSummary() {
  try {
    const summary = await Api.summary();
    $('stats').replaceChildren(...STAT_LABELS.map(pair =>
      el('div', { class: 'stat' }, el('strong', {}, summary[pair[0]]), el('span', {}, pair[1]))));
  } catch (err) {
    showErrors(err);
  }
}

// ---------- report controls ----------

// Fills the second dropdown to match the chosen report type.
function updateTargetList() {
  const type = $('reportType').value;
  const target = $('target');
  target.replaceChildren();
  if (type === 'daily') {
    $('targetLabel').textContent = 'Day';
    DAYS.forEach(day => target.append(el('option', { value: day }, dayName(day))));
  } else if (type === 'room') {
    $('targetLabel').textContent = 'Room';
    roomChoices.forEach(r => target.append(el('option', { value: r.roomId }, r.roomId + ' - ' + r.name)));
  } else {
    $('targetLabel').textContent = 'Instructor';
    instructorChoices.forEach(i => target.append(el('option', { value: i.instructorId }, i.instructorId + ' - ' + i.fullName)));
  }
}

// ---------- drawing results ----------

function bookingTable(list, showDay) {
  const head = el('tr', {}, [showDay ? el('th', {}, 'Day') : null,
    ...['Time', 'Course', 'Instructor', 'Room'].map(text => el('th', {}, text))]);
  const rows = list.map(s => el('tr', {}, [
    showDay ? el('td', {}, dayName(s.day)) : null,
    el('td', {}, timeRange(s.startTime, s.endTime)),
    el('td', {}, s.courseCode + ' - ' + s.courseTitle),
    el('td', {}, s.instructorName),
    el('td', {}, s.roomId + ' - ' + s.roomName)
  ]));
  return el('div', { class: 'table-wrap' }, el('table', {}, el('thead', {}, head), el('tbody', {}, rows)));
}

// Used for room and instructor reports: { MONDAY: [...], TUESDAY: [...] }
function drawGrouped(grouped) {
  const days = DAYS.filter(day => grouped[day] && grouped[day].length > 0);
  if (days.length === 0) {
    $('results').replaceChildren(el('p', { class: 'empty' }, 'There are no bookings.'));
    return;
  }
  $('results').replaceChildren(...days.flatMap(day => [
    el('h3', { class: 'day-heading' }, dayName(day)),
    bookingTable(grouped[day], false)
  ]));
}

async function showReport(event) {
  event.preventDefault();
  clearMessages();
  await guarded($('showBtn'), async () => {
    const type = $('reportType').value;
    const target = $('target').value;
    if (type === 'daily') {
      const list = await Api.dailyReport(target);
      $('results').replaceChildren(list.length === 0
        ? el('p', { class: 'empty' }, 'There are no bookings on ' + dayName(target) + '.')
        : bookingTable(list, false));
    } else if (type === 'room') {
      drawGrouped(await Api.roomReport(target));
    } else {
      drawGrouped(await Api.instructorReport(target));
    }
  });
}

// ---------- start-up ----------

async function init() {
  renderSidebar('reports.html');
  try {
    roomChoices = await Api.rooms();
    instructorChoices = await Api.instructors();
  } catch (err) {
    showErrors(err);
  }
  updateTargetList();
  $('reportType').addEventListener('change', updateTargetList);
  $('reportForm').addEventListener('submit', showReport);
  await loadSummary();
}

init();
