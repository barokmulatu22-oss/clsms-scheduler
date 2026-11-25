// courses.js - Courses page: table with search, add, edit and delete.

let editingCode = null; // course code being edited, or null when adding

function courseFromForm() {
  return {
    courseCode: $('courseCode').value,
    title: $('title').value,
    expectedEnrollment: Number($('enrollment').value),
    requiredRoomType: $('requiredType').value || null,
    department: $('department').value,
    section: $('section').value
  };
}

function resetForm() {
  editingCode = null;
  $('courseForm').reset();
  $('courseCode').disabled = false;
  $('formTitle').textContent = 'Add course';
  $('saveBtn').textContent = 'Add course';
  $('stopEditBtn').hidden = true;
}

function startEdit(course) {
  clearMessages();
  editingCode = course.courseCode;
  $('courseCode').value = course.courseCode;
  $('courseCode').disabled = true; // the code cannot change
  $('title').value = course.title;
  $('enrollment').value = course.expectedEnrollment;
  $('requiredType').value = course.requiredRoomType || '';
  $('department').value = course.department || '';
  $('section').value = course.section || '';
  $('formTitle').textContent = 'Edit course ' + course.courseCode;
  $('saveBtn').textContent = 'Save changes';
  $('stopEditBtn').hidden = false;
  $('title').focus();
}

async function removeCourse(course) {
  if (!confirm('Delete course ' + course.courseCode + '? This cannot be undone.')) return;
  clearMessages();
  try {
    const result = await Api.deleteCourse(course.courseCode);
    showMessage(result.message);
    if (editingCode === course.courseCode) resetForm();
    await loadCourses();
  } catch (err) {
    showErrors(err); // e.g. the course still has bookings
  }
}

async function loadCourses() {
  try {
    const courses = await Api.courses($('search').value);
    fillRows($('rows'), courses.map(course => [
      course.courseCode,
      course.title,
      course.expectedEnrollment,
      course.requiredRoomType
        ? el('span', { class: 'badge ' + course.requiredRoomType.toLowerCase() }, typeLabel(course.requiredRoomType))
        : 'Any type',
      course.department,
      course.section,
      [
        el('button', { type: 'button', class: 'btn secondary small', onclick: () => startEdit(course) }, 'Edit'),
        ' ',
        el('button', { type: 'button', class: 'btn danger small', onclick: () => removeCourse(course) }, 'Delete')
      ]
    ]), 7, 'No courses found.');
  } catch (err) {
    fillRows($('rows'), [], 7, 'The courses could not be loaded.');
    showErrors(err);
  }
}

async function saveCourse(event) {
  event.preventDefault();
  clearMessages();
  await guarded($('saveBtn'), async () => {
    const wasEditing = editingCode !== null;
    const saved = wasEditing
      ? await Api.updateCourse(editingCode, courseFromForm())
      : await Api.createCourse(courseFromForm());
    showMessage('Course ' + saved.courseCode + (wasEditing ? ' updated.' : ' added.'));
    resetForm();
    await loadCourses();
  });
  // On failure the form keeps what the user typed.
}

renderSidebar('courses.html');
$('courseForm').addEventListener('submit', saveCourse);
$('stopEditBtn').addEventListener('click', () => { clearMessages(); resetForm(); });
$('search').addEventListener('input', loadCourses);
loadCourses();
