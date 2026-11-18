const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_123";
const BOB_UID = "bob_student_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write student profile or subjects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("subjects").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("subjects").doc("subj_1").set({
      userId: ALICE_UID,
      name: "الرياضيات",
      colorHex: "#38BDF8",
      difficulty: 4,
      priority: 5,
      studyMinutesLogged: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Owner: can create and update profile, subjects, lessons, schedule, exams, focus sessions, notes, holidays", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // Profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").set({
      userId: ALICE_UID,
      name: "أحمد محمد",
      educationalStage: "الثانوية العامة",
      grade: "الصف الثالث الثانوي",
      school: "مدرسة النصر",
      governorate: "القاهرة",
      studyGoal: "التفوق في كلية الهندسة",
      wakeTimeMinutes: 360,
      sleepTimeMinutes: 1380,
      schoolStartMinutes: 420,
      schoolEndMinutes: 840,
      commuteMinutes: 30,
      mealBreakMinutes: 30,
      xp: 0,
      level: 1,
      currentStreakDays: 0,
      longestStreakDays: 0,
      unlockedBadgesCsv: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update on profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").update({
      xp: 120,
      level: 2,
      updatedAt: updated,
    })
  );

  // Subject
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("subj_math").set({
      userId: ALICE_UID,
      name: "الرياضيات",
      teacherName: "أ. محمود",
      colorHex: "#38BDF8",
      difficulty: 4,
      priority: 5,
      studyMinutesLogged: 45,
      notesSummary: "التفاضل والتكامل",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Lesson
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lessons").doc("lesson_1").set({
      userId: ALICE_UID,
      subjectId: "subj_math",
      unitName: "الوحدة الأولى",
      chapterName: "النهايات",
      title: "نهاية الدالة عند نقطة",
      partLabel: "الجزء الأول",
      kind: "LESSON",
      status: "NOT_STARTED",
      estimatedMinutes: 45,
      actualMinutes: 0,
      pagesCount: 12,
      questionsSolved: 0,
      comprehensionPercent: 0,
      difficultyOrImportance: 4,
      reviewStage: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Schedule block
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("schedule_blocks").doc("blk_1").set({
      userId: ALICE_UID,
      dayOfWeek: 1,
      title: "درس رياضة",
      category: "PRIVATE_LESSON",
      subjectId: "subj_math",
      startMinutes: 1020,
      endMinutes: 1110,
      fixed: true,
      completed: false,
      orderIndex: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Exam
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("exams").doc("exam_1").set({
      userId: ALICE_UID,
      title: "امتحان شهر أكتوبر",
      subjectId: "subj_math",
      examDateEpochDay: 20415,
      timeMinutes: 540,
      requiredParts: "الوحدة الأولى",
      readinessPercent: 60,
      maxScore: 50.0,
      reminderEnabled: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Focus Session
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("focus_sessions").doc("sess_1").set({
      userId: ALICE_UID,
      subjectId: "subj_math",
      lessonId: "lesson_1",
      durationMinutes: 25,
      breakMinutes: 5,
      mode: "POMODORO",
      completedAtEpochMs: Date.now(),
      createdAt: created,
      updatedAt: created,
    })
  );

  // Note
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      subjectId: "subj_math",
      title: "ملخص القوانين",
      content: "قانون النهايات الشهير",
      attachmentType: "NONE",
      important: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Holiday
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("holidays").doc("hol_1").set({
      userId: ALICE_UID,
      dateEpochDay: 20420,
      title: "إجازة أسبوعية",
      kind: "HOLIDAY",
      blockAllStudy: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List subcollection queries
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("subjects").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("lessons").get());
});

test("Cross-user isolation & negative bounds: Bob cannot read/write Alice data, shadow fields & future timestamps rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("subj_1").set({
      userId: ALICE_UID,
      name: "الفيزياء",
      colorHex: "#0EA5E9",
      difficulty: 5,
      priority: 5,
      studyMinutesLogged: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob cannot read or modify Alice's subject
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subjects").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subjects").doc("subj_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("subjects").doc("subj_1").update({
      name: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field ("id") or unexpected field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("subj_bad").set({
      id: "subj_bad",
      userId: ALICE_UID,
      name: "الكيمياء",
      colorHex: "#10B981",
      difficulty: 3,
      priority: 3,
      studyMinutesLogged: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("subj_future").set({
      userId: ALICE_UID,
      name: "الأحياء",
      colorHex: "#10B981",
      difficulty: 3,
      priority: 3,
      studyMinutesLogged: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("subj_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("DailyTasks: owner CRUD & dateEpochDay query succeed; cross-user & invalid payloads fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);
  const todayEpochDay = Math.floor(Date.now() / 86400000);

  // Unauthenticated denied
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("daily_tasks").where("dateEpochDay", "==", todayEpochDay).get()
  );

  // Owner create DailyTask
  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("daily_tasks").doc("task_1");
  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      title: "حل تمارين التفاضل",
      description: "20 مسألة من كتاب الوزارة",
      subjectId: "subj_math",
      subjectName: "الرياضيات",
      dateEpochDay: todayEpochDay,
      scheduledStartMinutes: 1020,
      estimatedMinutes: 45,
      priority: 3,
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Owner query for current day
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_tasks").where("dateEpochDay", "==", todayEpochDay).get()
  );

  // Owner partial update (toggle completed)
  await assertSucceeds(
    taskRef.update({
      completed: true,
      completedAtEpochMs: Date.now(),
      updatedAt: updated,
    })
  );

  // Bob denied reading, querying, or updating Alice's daily tasks
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("daily_tasks").where("dateEpochDay", "==", todayEpochDay).get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("daily_tasks").doc("task_1").update({
      completed: false,
      updatedAt: updated,
    })
  );

  // Reject ghost "id" field or invalid priority
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_tasks").doc("task_bad").set({
      id: "task_bad",
      userId: ALICE_UID,
      title: "مهمة غير صالحة",
      dateEpochDay: todayEpochDay,
      estimatedMinutes: 30,
      priority: 2,
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_tasks").doc("task_bad_prio").set({
      userId: ALICE_UID,
      title: "أولوية خارج النطاق",
      dateEpochDay: todayEpochDay,
      estimatedMinutes: 30,
      priority: 9,
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Owner delete
  await assertSucceeds(taskRef.delete());
});
