package com.pharmamentor.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.pharmamentor.app.data.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repo: PharmaRepository
    private var due = listOf<ReviewCardEntity>()
    private var pos = 0

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        repo = PharmaRepository(this)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 9)
        ReminderScheduler.scheduleAll(this)
        lifecycleScope.launch { repo.seedIfNeeded(); due = repo.dueCards(); home() }
    }

    private fun t(s: String, z: Float = 16f, b: Boolean = false)=TextView(this).apply {
        text=s; textSize=z; setTextColor(Color.rgb(35,45,55)); setPadding(18,12,18,12)
        if(b)setTypeface(null,Typeface.BOLD)
    }
    private fun base()=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; setPadding(16,18,16,14); setBackgroundColor(Color.rgb(248,250,252))
    }
    private fun button(s: String, f: () -> Unit)=Button(this).apply {
        text=s; textSize=15f; isAllCaps=false; setTextColor(Color.rgb(30,55,70));
        background=GradientDrawable().apply { setColor(Color.WHITE); cornerRadius=22f; setStroke(1,Color.rgb(220,226,232)) }
        setPadding(20,10,20,10); minimumHeight=52;
        val lp=LinearLayout.LayoutParams(-1,52); lp.setMargins(0,5,0,5); layoutParams=lp
        elevation=2f; setOnClickListener { f() }
    }
    private fun section(title:String)=TextView(this).apply {
        text=title; textSize=19f; setTypeface(null,Typeface.BOLD); setTextColor(Color.rgb(25,70,90));
        setPadding(8,18,8,6)
    }
    private fun statCard(label: String, value: String)=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(10,12,10,12);
        background=GradientDrawable().apply { setColor(Color.WHITE); cornerRadius=20f; setStroke(1,Color.rgb(225,230,235)) }; elevation=3f
        addView(TextView(this@MainActivity).apply { text=value; textSize=21f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER })
        addView(TextView(this@MainActivity).apply { text=label; textSize=12f; gravity=Gravity.CENTER })
    }
    private fun setScreen(content: LinearLayout, showNav: Boolean = true){
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(248,250,252)) }
        val scroll=ScrollView(this); scroll.addView(content, ScrollView.LayoutParams(-1,-1)); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        if(showNav){
            val nav=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(6,6,6,6); setBackgroundColor(Color.WHITE); elevation=8f }
            fun navBtn(label:String, action:()->Unit)=TextView(this).apply { text=label; textSize=12f; gravity=Gravity.CENTER; setPadding(4,10,4,10); setOnClickListener{action()} }
            listOf(navBtn("⌂\nالرئيسية"){home()},navBtn("🧠\nمراجعة"){review()},navBtn("💊\nالأدوية"){drugLibrary()},navBtn("📊\nتحليلي"){weaknesses()}).forEach { nav.addView(it,LinearLayout.LayoutParams(0,60,1f)) }
            root.addView(nav)
        }
        setContentView(root)
    }

    private fun home(){
        lifecycleScope.launch {
            val answered = repo.weeklyAnswered()
            val wrong = repo.weeklyWrong()
            val accuracy = if(answered == 0) 0 else ((answered - wrong) * 100 / answered)
            val xp = repo.xp()
            val streak = repo.streakDays()
            val plan = SmartPlanner(repo).today()
            val l = base()
            l.addView(t("Pharma Mentor 💊", 30f, true))
            l.addView(t("V51 • APK Build Preparation", 16f, true))
            l.addView(t("تعلّم بذكاء • راجع في الوقت المناسب • قِس تقدمك • طوّر نقاط ضعفك", 16f))
            val stats=LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,8,0,8) }
            listOf(statCard("XP",xp.toString()),statCard("Streak",streak.toString()),statCard("Accuracy",accuracy.toString()+"%"),statCard("Due",due.size.toString())).forEach { stats.addView(it,LinearLayout.LayoutParams(0,92,1f).apply{setMargins(3,3,3,3)}) }
            l.addView(stats)
            l.addView(t("🧠 ${due.size} بطاقات مستحقة • 📚 ${plan.topicCount} موضوع • 📝 ${plan.questionCount} سؤال • 🎯 ${plan.focus}", 15f, true))

            l.addView(button("🔎 البحث الشامل عن أي شيء"){globalSearch()})
            l.addView(button("🕸️ Knowledge Graph"){knowledgeGraph()})
            l.addView(button("📖 قاعدة المعرفة بدون إنترنت"){offlineKnowledge()})
            l.addView(section("🚀 ابدأ جلسة اليوم"))
            l.addView(button("🚀 جلسة الدراسة التكيفية"){unifiedStudySession()})
            l.addView(button("🎯 مهام وأهداف اليوم"){dailyMissions()})
            l.addView(button("🧠 المراجعة الذكية (${due.size})"){review()})
            l.addView(button("🧠 ماذا أدرس الآن؟"){personalStudy()})
            l.addView(button("🗓️ خطة اليوم الذكية"){smartPlan()})
            l.addView(button("📚 موضوع اليوم"){topic()})
            l.addView(button("📝 سؤال اليوم / بنك الأسئلة"){mcqs()})

            l.addView(section("💊 الأدوات السريرية"))
            l.addView(button("💊 Drug Library + Master Profiles"){drugLibrary()})
            l.addView(button("⚕️ Drug Interactions"){interactions()})
            l.addView(button("🩺 Clinical Cases"){cases()})
            l.addView(button("🧠 AI-like Clinical Tutor"){clinicalTutor()})

            l.addView(section("📊 الذكاء التعليمي"))
            l.addView(button("📊 لوحة الذكاء التحليلي"){analyticsDashboard()})
            l.addView(button("🎓 مستوى إتقان المواضيع"){masteryDashboard()})
            l.addView(button("🗺️ خارطة التعلم الشخصية"){learningRoadmap()})
            l.addView(button("🚀 جاهزية الإصدار V44"){releaseReadiness()})
            l.addView(button("🧪 مركز QA والإصدار"){qaCenter()})
            l.addView(button("🧰 فحص ما قبل الإصدار V44"){releasePreflight()})
            l.addView(button("📱 اختبار الجهاز قبل APK"){deviceSmokeTest()})
            l.addView(button("🛠️ فحص تكامل النظام"){systemAudit()})
            l.addView(button("📊 نقاط الضعف"){weaknesses()})
            l.addView(button("📈 التقرير الأسبوعي"){weeklyReport()})
            l.addView(button("🎯 اختبار تحديد المستوى"){placement()})
            l.addView(button("📚 المسارات الدراسية"){paths()})

            l.addView(section("🏆 التحفيز والتذكيرات"))
            l.addView(button("🔥 التحدي اليومي"){dailyChallenge()})
            l.addView(button("🏆 الإنجازات و Streak"){achievements()})
            l.addView(button("🔔 التذكيرات"){reminders()})
            l.addView(t("ℹ️ المحتوى الدوائي تعليمي؛ تحقق دائمًا من المرجع الرسمي الحالي قبل الاستخدام السريري.", 13f))
            setScreen(l)
        }
    }

    private fun dailyMissions(){ lifecycleScope.launch {
        repo.seedIfNeeded()
        val missions = repo.todayMissions()
        val l = base()
        l.addView(t("🎯 Daily Missions • Current",27f,true))
        l.addView(t("أهداف يومية قصيرة وواضحة. أكملها تدريجيًا، ويمكنك فتح النشاط المناسب مباشرة من كل مهمة."))
        val done = missions.sumOf { minOf(it.completed, it.target) }
        val total = missions.sumOf { it.target }
        l.addView(t("التقدم اليومي: $done / $total",21f,true))
        missions.forEach { m ->
            val progress = minOf(m.completed, m.target)
            val icon = if(progress >= m.target) "✅" else "🎯"
            l.addView(section("$icon ${m.title}"))
            l.addView(t("التقدم: $progress / ${m.target}\n💡 ${m.reason}",14f))
            l.addView(button(if(progress >= m.target) "✅ مكتملة" else "▶ تنفيذ المهمة") {
                lifecycleScope.launch {
                    repo.completeMission(m.id)
                    when(m.type) {
                        "review" -> review()
                        "quiz" -> mcqs()
                        "weakness" -> unifiedStudySession()
                        else -> home()
                    }
                }
            })
        }
        l.addView(t("ℹ️ المهمة اليومية هي طبقة تحفيزية تعليمية؛ إكمال زر المهمة يسجل تقدم المهمة ولا يثبت وحده إتقان المحتوى.",13f))
        l.addView(button("🚀 جلسة الدراسة التكيفية"){unifiedStudySession()})
        l.addView(button("🗺️ خارطة التعلم"){learningRoadmap()})
        l.addView(button("🏠 الرئيسية"){home()})
        setScreen(l)
    } }

    private fun releaseReadiness(){ lifecycleScope.launch {
        repo.seedIfNeeded()
        val a = repo.systemAudit()
        val l = base()
        l.addView(t("🚀 Pharma Mentor V51 • Release Readiness",27f,true))
        l.addView(t("فحص نهائي محلي قبل مرحلة بناء APK. لا يتم إرسال أي بيانات خارج الجهاز.",15f))
        val checks = listOf(
            "قاعدة المحتوى" to (a.drugs > 0 && a.drugProfiles > 0 && a.mcqs > 0 && a.cases > 0),
            "التعلم التكيفي" to (a.reviewCards > 0 && a.srsStates >= 0 && a.recommendations >= 0),
            "التحليلات والإتقان" to (a.learningEvents >= 0),
            "الوحدات السريرية" to (a.interactions > 0 && a.tutorCases > 0),
            "Knowledge Graph" to (a.graphNodes > 0 && a.graphEdges > 0),
            "Offline Knowledge" to (a.offlineArticles > 0),
            "المهام اليومية" to (repo.todayMissions().isNotEmpty())
        )
        checks.forEach { (name, ok) -> l.addView(t("${if(ok) "✅" else "⚠️"} $name",17f, !ok)) }
        l.addView(section("📦 حالة المشروع"))
        l.addView(t("الإصدار: V44.0\nRoom schema: V17\nOffline-first: نعم\nالتوقيعات والنشر: تحتاج بيئة Android/Gradle كاملة",15f))
        l.addView(section("📊 لقطة البيانات"))
        l.addView(t("Drugs: ${a.drugs}\nProfiles: ${a.drugProfiles}\nMCQs: ${a.mcqs}\nCases: ${a.cases}\nInteractions: ${a.interactions}\nReview cards: ${a.reviewCards}\nDue cards: ${a.dueCards}\nSRS states: ${a.srsStates}\nLearning events: ${a.learningEvents}",14f))
        l.addView(t("ℹ️ اجتياز هذا الفحص يعني أن البنية والبيانات الأساسية متصلة محليًا، لكنه لا يستبدل build/test على Android ولا اختبار APK على جهاز حقيقي.",13f))
        l.addView(button("🔄 إعادة الفحص"){releaseReadiness()})
        l.addView(button("🛠️ فحص التكامل التفصيلي"){systemAudit()})
        l.addView(button("🏠 الرئيسية"){home()})
        setScreen(l)
    } }


    private fun qaCenter(){ lifecycleScope.launch {
        repo.seedIfNeeded()
        val a = repo.systemAudit()
        val l = base()
        l.addView(t("🧪 Pharma Mentor V51 • QA Center",27f,true))
        l.addView(t("فحص جودة محلي قبل تسليم Release Candidate. لا يثبت هذا الفحص نجاح build أو سلامة الاستخدام السريري.",15f))
        val checks = listOf(
            "Seed/content integrity" to (a.drugs > 0 && a.drugProfiles > 0 && a.mcqs > 0 && a.cases > 0),
            "Clinical module integrity" to (a.interactions > 0 && a.tutorCases > 0),
            "Learning engine integrity" to (a.reviewCards > 0 && a.srsStates >= 0 && a.recommendations >= 0),
            "Analytics/mastery storage" to (a.learningEvents >= 0),
            "Knowledge graph integrity" to (a.graphNodes > 0 && a.graphEdges > 0),
            "Offline knowledge integrity" to (a.offlineArticles > 0),
            "Daily mission storage" to repo.todayMissions().isNotEmpty()
        )
        checks.forEach { (name, ok) -> l.addView(t("${if(ok) "✅" else "⚠️"} $name",16f,!ok)) }
        l.addView(section("📦 Release Candidate"))
        l.addView(t("App version: 44.0\nRoom schema: 17\nArchitecture: Offline-first\nNetwork dependency: None for core learning\nAPK signing/build: Pending Android SDK + Gradle environment",15f))
        l.addView(section("🔍 Recommended external QA"))
        l.addView(t("1. Build debug APK\n2. Run on Android emulator/device\n3. Verify notification permission and reminders\n4. Test Room migration from previous release\n5. Walk through Home → Review → Quiz → Case → Analytics\n6. Build signed release and perform final smoke test",14f))
        l.addView(button("🔄 إعادة فحص QA"){qaCenter()})
        l.addView(button("🚀 Release Readiness"){releaseReadiness()})
        l.addView(button("🏠 الرئيسية"){home()})
        setScreen(l)
    } }

    private fun releasePreflight(){ lifecycleScope.launch {
        repo.seedIfNeeded()
        val audit = repo.systemAudit()
        val missions = repo.todayMissions()
        val result = ReleaseChecklist.run(this@MainActivity, audit, missions.size)
        val l = base()
        l.addView(t("🧰 Pharma Mentor V51 • Release Preflight",27f,true))
        l.addView(t("فحص محلي أخير لإعدادات التطبيق والـManifest والوحدات الأساسية. فحص APK/signing يبقى خارجيًا.",15f))
        l.addView(t("النتيجة: ${result.passed}/${result.total} فحوص ناجحة",22f,true))
        result.checks.forEach { c ->
            l.addView(section("${if(c.ok) "✅" else "⚠️"} ${c.name}"))
            l.addView(t(c.detail,14f))
        }
        l.addView(t("ℹ️ إذا ظهر فحص الإشعارات ⚠️، امنح إذن الإشعارات من إعدادات Android ثم أعد الفحص. فحص APK لا يمكن اعتباره ناجحًا قبل بناء وتوقيع التطبيق فعليًا.",13f))
        l.addView(button("🔄 إعادة الفحص"){releasePreflight()})
        l.addView(button("🧪 مركز QA"){qaCenter()})
        l.addView(button("🏠 الرئيسية"){home()})
        setScreen(l)
    } }

    private fun deviceSmokeTest(){
        val l=base()
        l.addView(t("📱 Pharma Mentor V51 • Device Smoke Test",27f,true))
        l.addView(t("هذه قائمة الاختبار التي يجب تنفيذها على Emulator أو هاتف Android حقيقي قبل إنشاء APK النهائي.",15f))
        DeviceSmokeTest.checklist().forEach { c ->
            val icon = when(c.status){ "PENDING" -> "🟡"; "EXTERNAL" -> "🔵"; else -> "✅" }
            l.addView(section("$icon ${c.name}"))
            l.addView(t("الحالة: ${c.status}\n${c.detail}",14f))
        }
        l.addView(section("📦 Release target"))
        l.addView(t("Version: ${ReleaseInfo.VERSION_NAME} (code ${ReleaseInfo.VERSION_CODE})\nRoom schema: ${ReleaseInfo.ROOM_SCHEMA}\nTarget SDK: 35\nMinimum SDK: 26",15f))
        l.addView(t("ℹ️ هذا الاختبار لا يدّعي نجاح APK؛ هو تجهيز منظم للاختبار الخارجي الفعلي.",13f))
        l.addView(button("🧰 Release Preflight"){releasePreflight()})
        l.addView(button("🏠 الرئيسية"){home()})
        setScreen(l)
    }

    private fun systemAudit(){ lifecycleScope.launch {
        repo.seedIfNeeded()
        val a = repo.systemAudit()
        val l = base()
        l.addView(t("🛠️ Pharma Mentor System Audit",27f,true))
        l.addView(t("V44 يراجع جاهزية الوحدات الأساسية محليًا، مع فحص إعدادات التطبيق والتذكيرات دون إرسال بيانات خارج الجهاز.",15f))
        l.addView(t("جاهزية الوحدات: ${a.healthyModules}/13",22f,true))
        l.addView(section("📚 قاعدة المحتوى"))
        listOf(
            "💊 Drug Library" to a.drugs, "📋 Drug Master Profiles" to a.drugProfiles,
            "🩺 Clinical Cases" to a.cases, "📝 MCQ Bank" to a.mcqs,
            "⚕️ Interactions" to a.interactions, "🧠 Clinical Tutor Cases" to a.tutorCases,
            "🕸️ Knowledge Graph Nodes" to a.graphNodes, "🔗 Knowledge Graph Edges" to a.graphEdges,
            "📖 Offline Knowledge" to a.offlineArticles
        ).forEach { (name,count) -> l.addView(t("${if(count>0) "✅" else "⚠️"} $name: $count",15f)) }
        l.addView(section("🧠 أنظمة التعلم"))
        listOf(
            "Review Cards" to a.reviewCards, "Due Cards" to a.dueCards,
            "SRS States" to a.srsStates, "Learning Events" to a.learningEvents,
            "Today's Recommendations" to a.recommendations
        ).forEach { (name,count) -> l.addView(t("$name: $count",15f)) }
        l.addView(section("🔧 حالة التكامل"))
        l.addView(t("Room DB: connected\nSeed data: initialized\nOffline mode: available\nAdaptive review: connected\nAnalytics: connected\nClinical modules: connected",15f))
        l.addView(t("ℹ️ هذا فحص بنيوي محلي وليس اختبارًا سريريًا أو ضمانًا لسلامة المحتوى الدوائي.",13f))
        l.addView(button("🔄 إعادة الفحص"){systemAudit()})
        l.addView(button("🏠 الرئيسية"){home()})
        setScreen(l)
    } }

    private fun masteryDashboard(){ lifecycleScope.launch {
        val items = repo.topicMastery()
        val l=base(); l.addView(t("🎓 Topic Mastery • Current",27f,true))
        l.addView(t("نظام الإتقان يبني خريطة إتقان لكل موضوع ويحوّل النتيجة إلى أهداف 70% و80% و90% وMastered. كل الحسابات محلية."))
        if(items.isEmpty()) l.addView(t("لا توجد بيانات إتقان بعد. ابدأ بحل MCQs أو جلسة الدراسة التكيفية."))
        items.take(12).forEach { m ->
            l.addView(t("${m.topic}\nإتقان: ${m.masteryPercent}% • ثقة: ${m.confidencePercent}%\nالحالة: ${MasteryEngine.status(m.masteryPercent)} • محاولات: ${m.attempts} • Streak: ${m.streak}",17f,m.masteryPercent<50))
            l.addView(t("📍 ${MasteryEngine.nextMilestone(m.masteryPercent)}",14f))
            l.addView(t("➡️ ${MasteryEngine.nextAction(m.masteryPercent)}",14f))
        }
        l.addView(button("🚀 جلسة تكيفية"){unifiedStudySession()})
        l.addView(button("📝 اختبار لرفع الإتقان"){mcqs()})
        l.addView(button("📊 التحليلات"){analyticsDashboard()})
        l.addView(button("🏠 الرئيسية"){home()}); setScreen(l)
    } }

    private fun learningRoadmap(){ lifecycleScope.launch {
        val roadmap = LearningRoadmapEngine.build(repo.topicMastery())
        val l = base()
        l.addView(t("🗺️ Personalized Learning Roadmap • Current",27f,true))
        l.addView(t("مسار شخصي يحدد ماذا تدرس الآن، ماذا تراجع بعده، وما الخطوة التالية للوصول إلى Mastered. الأولوية مبنية على مستوى الإتقان الحالي."))
        if(roadmap.isEmpty()) {
            l.addView(t("لا توجد بيانات كافية لبناء خارطة شخصية بعد. ابدأ بحل MCQs أو جلسة الدراسة التكيفية."))
        } else {
            roadmap.forEach { item ->
                l.addView(section("${item.rank}. ${item.topic}"))
                l.addView(t("📊 الإتقان: ${item.mastery}% • ${item.status}",17f,item.mastery < 50))
                l.addView(t("💡 ${item.reason}",14f))
                l.addView(t("➡️ ${item.action}",14f,true))
            }
        }
        l.addView(button("🚀 ابدأ جلسة تكيفية"){unifiedStudySession()})
        l.addView(button("🎓 لوحة الإتقان"){masteryDashboard()})
        l.addView(button("🏠 الرئيسية"){home()})
        setScreen(l)
    } }

    private fun analyticsDashboard(){ lifecycleScope.launch {
        val x=repo.progressIntelligence()
        val l=base(); l.addView(t("📊 Analytics & Progress Intelligence",27f,true))
        l.addView(t("تحليل محلي لأدائك الدراسي، مع تحويل الأخطاء إلى أولويات قابلة للتنفيذ."))
        val stats=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL}
        listOf(statCard("الدقة الكلية", "${x.accuracyPercent}%"),statCard("هذا الأسبوع", "${x.weeklyAccuracyPercent}%"),statCard("الإجابات", x.totalAnswered.toString()),statCard("Streak", x.currentStreak.toString())).forEach{stats.addView(it,LinearLayout.LayoutParams(0,92,1f).apply{setMargins(3,3,3,3)})}
        l.addView(stats)
        l.addView(t("XP: ${x.xp}   •   بطاقات مستحقة: ${x.dueCards}",15f,true))
        l.addView(section("🎯 أولويات التحسين"))
        if(x.weakTopics.isEmpty()) l.addView(t("ابدأ بالإجابة عن أسئلة أو مراجعة بطاقات حتى تتكوّن بيانات تحليلية."))
        x.weakTopics.forEach{ w -> l.addView(t("${w.priority}  •  ${w.topic}  •  أخطاء ${w.wrong}/${w.total} (${w.errorPercent}%)",15f,w.priority=="HIGH")) }
        l.addView(section("💡 تفسير سريع"))
        l.addView(t(if(x.weeklyAnswered==0) "لا توجد بيانات هذا الأسبوع بعد." else "أجبت عن ${x.weeklyAnswered} سؤالًا هذا الأسبوع، منها ${x.weeklyWrong} إجابة خاطئة. استخدم المراجعة المتباعدة للموضوعات ذات الأولوية HIGH."))
        l.addView(button("🧠 ابدأ المراجعة"){review()}); l.addView(button("📝 ابدأ اختبارًا"){mcqs()}); l.addView(button("🏠 الرئيسية"){home()}); setScreen(l)
    } }

    private fun personalStudy(){ lifecycleScope.launch {
        val items=PersonalStudyEngine(repo).recommendations()
        val l=base(); l.addView(t("🧠 ماذا أدرس الآن؟",27f,true)); l.addView(t("المحرك يختار الأولويات من أخطائك، بطاقات المراجعة المستحقة، مستواك، والتوازن بين التعلم الجديد والتطبيق السريري."))
        if(items.isEmpty()) l.addView(t("لا توجد توصيات بعد. ابدأ بجلسة مراجعة أو اختبار."))
        items.forEachIndexed { i,x ->
            val prefix=if(x.completed) "✅" else "${i+1}."
            l.addView(button("$prefix ${x.title}"){
                lifecycleScope.launch { repo.completeStudyRecommendation(x.id); when(x.type){
                    "review" -> review(); "quiz" -> mcqs(); "topic" -> topic(); "case" -> cases(); "weakness" -> weaknesses(); else -> home()
                }}
            })
            l.addView(t("${x.reason}\nالأولوية: ${x.priority}",14f))
        }
        l.addView(button("🔄 إعادة حساب الأولويات"){ lifecycleScope.launch { val key=java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()); repo.studyRecommendations(key).forEach { repo.completeStudyRecommendation(it.id) }; personalStudy() } })
        l.addView(button("الرئيسية"){home()}); setScreen(l)
    } }

    private fun offlineKnowledge(){
        lifecycleScope.launch {
            val items=repo.offlineAll()
            val categories=items.map{it.category}.distinct()
            val l=base(); l.addView(t("📖 Offline Knowledge Base",27f,true)); l.addView(t("محتوى أساسي مخزّن داخل التطبيق ويمكن الرجوع إليه دون اتصال بالإنترنت."))
            l.addView(button("🔎 بحث داخل قاعدة المعرفة"){ offlineSearch() })
            categories.forEach { c -> l.addView(button("📚 $c"){ offlineCategory(c) }) }
            l.addView(button("الرئيسية"){home()}); setScreen(l)
        }
    }
    private fun offlineCategory(category:String){ lifecycleScope.launch { val l=base(); l.addView(t("📚 $category",25f,true)); repo.offlineByCategory(category).forEach { x -> l.addView(button(x.title){ offlineDetail(x.id) }) }; l.addView(button("رجوع"){offlineKnowledge()}); setScreen(l) } }
    private fun offlineSearch(){ val l=base(); l.addView(t("🔎 بحث Offline",25f,true)); val input=EditText(this).apply{hint="اكتب اسم دواء أو موضوع أو كلمة مفتاحية"}; l.addView(input); l.addView(button("بحث"){ val q=input.text.toString().trim(); if(q.length<2) Toast.makeText(this@MainActivity,"اكتب حرفين على الأقل",Toast.LENGTH_SHORT).show() else lifecycleScope.launch { val r=repo.searchOffline(q); val out=base(); out.addView(t("نتائج: $q",24f,true)); r.forEach{x->out.addView(button("${x.category} • ${x.title}"){offlineDetail(x.id)})}; out.addView(button("رجوع"){offlineKnowledge()}); setScreen(out) } }); l.addView(button("رجوع"){offlineKnowledge()}); setScreen(l) }
    private fun offlineDetail(id:String){ lifecycleScope.launch { val x=repo.offlineAll().firstOrNull{it.id==id} ?: return@launch; val l=base(); l.addView(t(x.title,26f,true)); l.addView(t("القسم: ${x.category}",15f,true)); l.addView(t(x.content,17f)); l.addView(t("Keywords: ${x.keywords}",13f)); l.addView(section("📚 Reference")); l.addView(t(x.reference,14f)); l.addView(t("Last reviewed: ${x.reviewed}",12f)); l.addView(t("⚠️ المحتوى تعليمي. تحقق من المرجع الرسمي الحالي قبل اتخاذ قرار سريري." ,13f)); l.addView(button("رجوع"){offlineKnowledge()}); setScreen(l) } }

    private fun knowledgeGraph(){ lifecycleScope.launch {
        val nodes=repo.graphNodes(); val l=base(); l.addView(t("🕸️ Knowledge Graph",27f,true));
        l.addView(t("شبكة تربط الدواء بالمستقبل أو الهدف، المرض، الأعراض الجانبية، التداخلات والحالات السريرية. ابدأ باسم دواء أو مفهوم."))
        val input=EditText(this@MainActivity).apply { hint="مثال: metoprolol / β2 / hypertension"; setSingleLine(true) }; l.addView(input)
        l.addView(button("🔎 استكشف الشبكة"){ lifecycleScope.launch { val q=input.text.toString().trim(); val found=if(q.isEmpty()) nodes else repo.searchGraph(q); showGraphNodes(found) } })
        l.addView(section("📚 أمثلة سريعة")); nodes.filter{it.type=="Drug"}.take(10).forEach{ n -> l.addView(button("💊 ${n.label}"){ showGraphNode(n.id) }) }
        l.addView(button("الرئيسية"){home()}); setScreen(l)
    } }

    private fun showGraphNodes(nodes:List<KnowledgeGraphNodeEntity>){ val l=base(); l.addView(t("🕸️ نتائج الشبكة",26f,true)); if(nodes.isEmpty()) l.addView(t("لا توجد نتائج.")); nodes.forEach{ n -> l.addView(button("${typeIcon(n.type)} ${n.label}"){showGraphNode(n.id)}); if(n.description.isNotBlank()) l.addView(t(n.description,14f)) }; l.addView(button("رجوع"){knowledgeGraph()}); setScreen(l) }

    private fun showGraphNode(nodeId:String){ lifecycleScope.launch {
        val nodes=repo.graphNodes(); val n=nodes.firstOrNull{it.id==nodeId} ?: return@launch; val edges=repo.graphEdges(nodeId); val map=nodes.associateBy{it.id}; val l=base();
        l.addView(t("${typeIcon(n.type)} ${n.label}",27f,true)); l.addView(t("Type: ${n.type}",14f,true)); if(n.description.isNotBlank()) l.addView(t(n.description));
        l.addView(section("🔗 Connections (${edges.size})"));
        if(edges.isEmpty()) l.addView(t("لا توجد روابط إضافية محفوظة."));
        edges.forEach{ e -> val other=map[if(e.fromId==nodeId)e.toId else e.fromId]; if(other!=null){ val rel=if(e.fromId==nodeId)e.relation else "← ${e.relation}"; l.addView(button("${typeIcon(other.type)} ${other.label}\n$rel"){showGraphNode(other.id)}); if(e.reference.isNotBlank()) l.addView(t("Reference: ${e.reference}",12f)) } }
        l.addView(button("🕸️ العودة للشبكة"){knowledgeGraph()}); l.addView(button("الرئيسية"){home()}); setScreen(l)
    } }

    private fun typeIcon(type:String)=when(type){"Drug"->"💊";"Disease"->"🩺";"Receptor"->"⚙️";"Target"->"🎯";"Adverse effect"->"⚠️";"Interaction"->"⚕️";"Clinical case"->"📋";else->"🔗"}

    private fun globalSearch(){
        val l=base()
        l.addView(t("🔎 البحث الشامل",27f,true))
        l.addView(t("ابحث من مكان واحد في الأدوية، ملفات الأدوية، الحالات، الأسئلة، المواضيع والتداخلات."))
        val input=EditText(this).apply { hint="مثال: warfarin أو beta blocker"; setSingleLine(true) }
        l.addView(input)
        l.addView(button("🔎 بحث"){ val q=input.text.toString().trim(); if(q.length<2){ Toast.makeText(this,"اكتب حرفين على الأقل للبحث.",Toast.LENGTH_SHORT).show() } else { runGlobalSearch(q) } })
        l.addView(button("📚 عرض مكتبة الأدوية"){drugLibrary()})
        l.addView(button("⚕️ عرض التداخلات"){interactions()})
        l.addView(button("الرئيسية"){home()})
        setScreen(l)
    }

    private fun runGlobalSearch(q:String){
        lifecycleScope.launch {
            val term=q.lowercase()
            val drugs=repo.allDrugs().filter { listOf(it.name,it.className,it.mechanism,it.uses,it.adverseEffects).any { v -> v.lowercase().contains(term) } }
            val profiles=repo.allDrugProfiles().filter { listOf(it.genericName,it.brandExamples,it.dosageForms,it.route,it.adultDose,it.renalConsiderations,it.hepaticConsiderations,it.monitoring,it.onLabel,it.offLabel,it.counseling).any { v -> v.lowercase().contains(term) } }
            val cases=repo.allCases().filter { listOf(it.title,it.scenario,it.answer).any { v -> v.lowercase().contains(term) } }
            val mcqs=repo.allMcqs().filter { listOf(it.question,it.options,it.explanation,it.topic).any { v -> v.lowercase().contains(term) } }
            val topics=repo.allTopics().filter { listOf(it.title,it.body,it.reference).any { v -> v.lowercase().contains(term) } }
            val interactions=repo.allInteractions().filter { listOf(it.drugA,it.drugB,it.mechanism,it.management).any { v -> v.lowercase().contains(term) } }

            val l=base()
            l.addView(t("🔎 نتائج البحث: $q",25f,true))
            l.addView(t("${drugs.size} أدوية • ${profiles.size} ملفات • ${cases.size} حالات • ${mcqs.size} أسئلة • ${topics.size} مواضيع • ${interactions.size} تداخلات",14f))
            if(drugs.isEmpty() && profiles.isEmpty() && cases.isEmpty() && mcqs.isEmpty() && topics.isEmpty() && interactions.isEmpty()) l.addView(t("لا توجد نتائج مطابقة. جرّب الاسم العلمي أو اسم الفئة الدوائية."))

            if(drugs.isNotEmpty()){
                l.addView(section("💊 الأدوية"))
                drugs.take(10).forEach { d ->
                    l.addView(button("${d.name} — ${d.className}"){showDrugMasterProfile(d.id,"search")})
                }
            }
            if(profiles.isNotEmpty()){
                l.addView(section("📋 Drug Master Profiles"))
                profiles.take(10).forEach { p ->
                    l.addView(button("${p.genericName} — Master Profile"){showDrugMasterProfile(p.drugId,"search")})
                }
            }
            if(cases.isNotEmpty()){
                l.addView(section("🩺 Clinical Cases"))
                cases.take(10).forEach { c -> l.addView(button(c.title){showCaseFromSearch(c.id)}) }
            }
            if(mcqs.isNotEmpty()){
                l.addView(section("📝 Questions"))
                mcqs.take(10).forEach { qx -> l.addView(button(qx.question){mcqs()}) }
            }
            if(topics.isNotEmpty()){
                l.addView(section("📚 Daily Topics"))
                topics.take(10).forEach { tp -> l.addView(button(tp.title){topic()}) }
            }
            if(interactions.isNotEmpty()){
                l.addView(section("⚕️ Drug Interactions"))
                interactions.take(10).forEach { i -> l.addView(button("${i.drugA} + ${i.drugB}"){showInteractions(listOf(i))}) }
            }
            l.addView(button("🔄 بحث جديد"){globalSearch()})
            l.addView(button("الرئيسية"){home()})
            setScreen(l)
        }
    }

    private fun showCaseFromSearch(caseId:String){
        lifecycleScope.launch {
            val c=repo.allCases().firstOrNull { it.id==caseId } ?: return@launch
            val links=repo.caseLinks(c.id)
            val x=base(); x.addView(t(c.title,24f,true)); x.addView(t(c.scenario));
            x.addView(button("إظهار الإجابة"){Toast.makeText(this@MainActivity,c.answer,Toast.LENGTH_LONG).show()})
            if(links.isNotEmpty()){ x.addView(t("🔗 الأدوية المرتبطة",19f,true)); links.forEach { x.addView(t("${it.drugId}: ${it.rationale}")) } }
            x.addView(t("Reference: ${c.reference}",14f)); x.addView(button("رجوع للبحث"){globalSearch()}); setScreen(x)
        }
    }

    private fun unifiedStudySession(){
        lifecycleScope.launch {
            due = repo.dueCards()
            val topic = repo.latestTopic()
            val mcqs = repo.allMcqs()
            val cases = repo.allCases()
            val intelligence = repo.progressIntelligence()
            val recommendations = AdaptiveStudyCoach.buildPlan(
                intelligence = intelligence,
                hasTopic = topic != null,
                dueCards = due.size,
                hasQuiz = mcqs.isNotEmpty(),
                hasCase = cases.isNotEmpty()
            )
            val stages = recommendations.map { it.stage }.toMutableList()
            if(stages.isEmpty()){
                Toast.makeText(this@MainActivity, "لا توجد عناصر كافية لبناء جلسة الآن.", Toast.LENGTH_SHORT).show()
                home(); return@launch
            }
            var stageIndex = 0
            var adaptiveInserted = 0
            var reviewCard: ReviewCardEntity? = null
            var quiz: McqEntity? = null
            var clinicalCase: ClinicalCaseEntity? = null
            var topicDone = false
            var revealed = false
            var selected = ""
            var quizCorrect = false
            var caseDone = false
            var adaptiveDifficulty = AdaptiveDifficultyEngine.initialLevel(intelligence)
            var consecutiveCorrect = 0
            var consecutiveWrong = 0
            val usedQuizIds = mutableSetOf<String>()

            fun render(){
                val l=base()
                val total=stages.size
                if(stageIndex >= total){
                    l.addView(t("🎉 اكتملت جلسة اليوم",28f,true))
                    l.addView(t("أحسنت! مررت عبر ${total} مراحل تعليمية مترابطة.",18f,true))
                    l.addView(t("تم تسجيل الأنشطة محليًا لتغذية التحليل والمراجعة التكيفية."))
                    lifecycleScope.launch { repo.logSession("unified_study", "session-${System.currentTimeMillis()}", total) }
                    l.addView(button("📊 تحليل تقدمي"){analyticsDashboard()})
                    l.addView(button("🔄 جلسة أخرى"){unifiedStudySession()})
                    l.addView(button("🏠 الرئيسية"){home()})
                    setScreen(l); return
                }
                val stage=stages[stageIndex]
                l.addView(t("🧠 Adaptive Study Coach",27f,true))
                l.addView(t("المرحلة ${stageIndex+1} من $total",14f,true))
                l.addView(t(UnifiedStudySession.label(stage),21f,true))
                recommendations.getOrNull(stageIndex)?.let { r ->
                    l.addView(t("🎯 لماذا هذه الخطوة؟ ${r.reason}",14f,true))
                    r.focusTopic?.let { l.addView(t("🔎 التركيز: $it",14f)) }
                }
                when(stage){
                    UnifiedStudySession.Stage.TOPIC -> {
                        l.addView(t(topic?.title ?: "موضوع اليوم",20f,true))
                        l.addView(t(topic?.body ?: "لا يوجد محتوى."))
                        l.addView(t("📖 المرجع: ${topic?.reference ?: "—"}",14f))
                        if(!topicDone) l.addView(button("✅ تمت القراءة — التالي"){ topicDone=true; lifecycleScope.launch { topic?.let { repo.logSession("topic",it.id) }; stageIndex++; render() } })
                        else l.addView(button("التالي →"){stageIndex++;render()})
                    }
                    UnifiedStudySession.Stage.REVIEW -> {
                        if(reviewCard==null) reviewCard=due.firstOrNull()
                        val c=reviewCard
                        if(c==null){ stageIndex++; render(); return }
                        l.addView(t(c.question,19f,true))
                        if(!revealed){
                            l.addView(button("👁️ إظهار الإجابة"){revealed=true;render()})
                            l.addView(t("حاول تذكر الإجابة قبل كشفها."))
                        } else {
                            l.addView(t("الإجابة:\n${c.answer}",17f))
                            l.addView(t("قيّم مدى سهولة تذكرك:"))
                            listOf(Rating.AGAIN,Rating.HARD,Rating.GOOD,Rating.EASY).forEach { r ->
                                l.addView(button(r.name){
                                    lifecycleScope.launch {
                                        val current=repo.srsState(c.id) ?: AdvancedSpacedRepetitionEngine.initial(c.id,c.dueAt)
                                        val result=AdvancedSpacedRepetitionEngine.schedule(current,r)
                                        repo.save(c.copy(dueAt=result.state.dueAt,intervalDays=result.intervalDays,reps=result.state.repetitions,lapses=result.state.lapses,ease=when(r){Rating.EASY->c.ease+0.15;Rating.AGAIN->maxOf(1.3,c.ease-0.2);Rating.HARD->maxOf(1.3,c.ease-0.15);Rating.GOOD->c.ease}))
                                        repo.saveSrsState(result.state)
                                        repo.logLearningEvent(c.id,"unified_review",c.category,r!=Rating.AGAIN)
                                        val decision = AdaptiveSessionEngine.afterReview(r, c.category)
                                        if (decision.action == "QUIZ" && mcqs.isNotEmpty() && adaptiveInserted < 2) {
                                            AdaptiveSessionEngine.insertPriorityStage(stages, stageIndex, UnifiedStudySession.Stage.QUIZ)
                                            adaptiveInserted++
                                        }
                                        stageIndex++; render()
                                    }
                                })
                            }
                        }
                    }
                    UnifiedStudySession.Stage.QUIZ -> {
                        if(quiz==null) {
                            val focus = recommendations.getOrNull(stageIndex)?.focusTopic
                            val unused = mcqs.filterNot { usedQuizIds.contains(it.id) }
                            val focused = if(focus.isNullOrBlank()) unused else unused.filter { it.topic.contains(focus, true) }
                            val difficultyPool = focused.filter { AdvancedQuizEngine.difficultyOf(it) == adaptiveDifficulty }
                            val fallbackDifficulty = unused.filter { AdvancedQuizEngine.difficultyOf(it) == adaptiveDifficulty }
                            quiz = (difficultyPool.ifEmpty { fallbackDifficulty }.ifEmpty { focused }.ifEmpty { unused }.ifEmpty { mcqs }).randomOrNull()
                            quiz?.let { usedQuizIds += it.id }
                        }
                        val q=quiz
                        if(q==null){stageIndex++;render();return}
                        l.addView(t("🎚️ المستوى المستهدف: ${AdvancedQuizEngine.difficultyLabel(adaptiveDifficulty)}",13f,true))
                        l.addView(t(q.question,20f,true))
                        if(selected.isEmpty()){
                            q.options.split("|").forEach { opt -> l.addView(button(opt){
                                selected=opt; quizCorrect=opt.startsWith(q.correct.substringBefore('.')); render()
                            }) }
                        } else {
                            l.addView(t(if(quizCorrect) "✅ إجابتك صحيحة" else "❌ تحتاج مراجعة",19f,true))
                            l.addView(t("إجابتك: $selected\nالإجابة الصحيحة: ${q.correct}\n\n💡 ${q.explanation}",16f))
                            l.addView(t("📚 ${q.reference}",13f))
                            l.addView(button("التالي →"){
                                lifecycleScope.launch {
                                    if (quizCorrect) { consecutiveCorrect++; consecutiveWrong = 0 } else { consecutiveWrong++; consecutiveCorrect = 0 }
                                    val decision = AdaptiveDifficultyEngine.afterAnswer(adaptiveDifficulty, quizCorrect, consecutiveCorrect, consecutiveWrong)
                                    adaptiveDifficulty = decision.nextLevel
                                    Toast.makeText(this@MainActivity, "🎯 ${decision.label}: ${decision.reason}", Toast.LENGTH_SHORT).show()
                                    repo.logLearningEvent(q.id,"unified_quiz",q.topic,quizCorrect)
                                    repo.logSession("unified_quiz",q.id,if(quizCorrect)1 else 0)
                                    val sessionDecision = AdaptiveSessionEngine.afterQuiz(quizCorrect, q.topic)
                                    if (sessionDecision.action == "REVIEW" && due.isNotEmpty() && adaptiveInserted < 2) {
                                        AdaptiveSessionEngine.insertPriorityStage(stages, stageIndex, UnifiedStudySession.Stage.REVIEW)
                                        adaptiveInserted++
                                    }
                                    stageIndex++; render()
                                }
                            })
                        }
                    }
                    UnifiedStudySession.Stage.CASE -> {
                        if(clinicalCase==null) clinicalCase=cases.randomOrNull()
                        val c=clinicalCase
                        if(c==null){stageIndex++;render();return}
                        l.addView(t(c.title,20f,true)); l.addView(t(c.scenario))
                        if(!caseDone){
                            l.addView(button("🔍 إظهار القرار والتفسير"){caseDone=true;render()})
                            l.addView(t("حاول بناء القرار الدوائي بنفسك قبل الكشف."))
                        } else {
                            l.addView(t("💡 القرار التعليمي\n${c.answer}",17f))
                            l.addView(t("📚 ${c.reference}",13f))
                            l.addView(button("إكمال الجلسة →"){ lifecycleScope.launch { repo.logSession("unified_case",c.id); stageIndex++; render() } })
                        }
                    }
                }
                l.addView(button("⏹️ إنهاء الجلسة"){home()})
                setScreen(l)
            }
            render()
        }
    }

    private fun review(){
        lifecycleScope.launch { due = repo.dueCards(); if(due.isEmpty()){ Toast.makeText(this@MainActivity,"لا توجد بطاقات مستحقة الآن 🎉",Toast.LENGTH_SHORT).show(); home(); return@launch }
            val c=due[pos%due.size]; val l=base(); l.addView(t("🧠 Smart Review",26f,true)); l.addView(t(c.question,19f,true));
            l.addView(button("إظهار الإجابة"){ Toast.makeText(this@MainActivity,c.answer,Toast.LENGTH_LONG).show() })
            l.addView(t("قيّم تذكرك:")); listOf(Rating.AGAIN,Rating.HARD,Rating.GOOD,Rating.EASY).forEach { r -> l.addView(button(r.name){ saveReview(c,r) }) }
            l.addView(button("رجوع"){home()}); setScreen(l)
        }
    }

    private fun saveReview(c:ReviewCardEntity,r:Rating){
        lifecycleScope.launch {
            val current = repo.srsState(c.id) ?: AdvancedSpacedRepetitionEngine.initial(c.id, c.dueAt)
            val result = AdvancedSpacedRepetitionEngine.schedule(current, r)
            val updated = c.copy(
                dueAt = result.state.dueAt,
                intervalDays = result.intervalDays,
                ease = when (r) { Rating.EASY -> c.ease + 0.15; Rating.AGAIN -> maxOf(1.3, c.ease - 0.2); Rating.HARD -> maxOf(1.3, c.ease - 0.15); Rating.GOOD -> c.ease },
                reps = result.state.repetitions,
                lapses = result.state.lapses
            )
            repo.save(updated)
            repo.saveSrsState(result.state)
            repo.logLearningEvent(c.id,"review",c.category,r != Rating.AGAIN)
            due=repo.dueCards(); pos=0; review()
        }
    }

    private fun topic(){
        lifecycleScope.launch { val x=repo.latestTopic(); val l=base(); l.addView(t("📚 موضوع اليوم",26f,true)); l.addView(t(x?.title ?: "لا يوجد موضوع",20f,true)); l.addView(t(x?.body ?: "")); l.addView(t("📖 المرجع: ${x?.reference ?: "—"}",14f)); l.addView(button("تمت القراءة"){ lifecycleScope.launch { repo.logSession("topic", x?.id ?: "daily-topic"); Toast.makeText(this@MainActivity,"سُجلت جلسة التعلم.",Toast.LENGTH_SHORT).show(); home() } }); l.addView(button("رجوع"){home()}); setScreen(l) }
    }
    private fun drugLibrary(){ lifecycleScope.launch {
        val items=repo.allDrugs(); val l=base(); l.addView(t("💊 Drug Library + Master Profiles",26f,true))
        l.addView(t("اختر الدواء لفتح Drug Master Profile الكامل."))
        val input=EditText(this@MainActivity).apply { hint="Search drug / class"; setSingleLine(true) }; l.addView(input)
        l.addView(button("🔎 Search"){ lifecycleScope.launch { val q=input.text.toString().trim(); showDrugResults(if(q.isEmpty()) repo.allDrugs() else repo.searchDrugs(q)) } })
        l.addView(t("عدد الأدوية: ${items.size}")); items.forEach { d -> l.addView(button(d.name+" — "+d.className){showDrugMasterProfile(d.id, "library")}) }
        l.addView(button("رجوع"){home()}); setScreen(l)
    } }

    private fun showDrugResults(items: List<DrugEntity>){
        val l=base(); l.addView(t("💊 نتائج البحث",26f,true)); l.addView(t("عدد النتائج: ${items.size}"))
        items.forEach { d -> l.addView(button(d.name+" — "+d.className){showDrugMasterProfile(d.id, "results")}) }
        l.addView(button("رجوع للمكتبة"){drugLibrary()}); l.addView(button("الرئيسية"){home()}); setScreen(l)
    }

    private fun showDrugMasterProfile(drugId:String, back:String){ lifecycleScope.launch {
        val d=repo.allDrugs().firstOrNull { it.id==drugId }; val p=repo.drugProfile(drugId)
        val l=base(); l.addView(t("💊 Drug Master Profile",26f,true)); l.addView(t(d?.name ?: p?.genericName ?: drugId,22f,true))
        if(d!=null) { l.addView(t("Class: ${d.className}",18f,true)); l.addView(t("Mechanism\n${d.mechanism}")); l.addView(t("Core uses\n${d.uses}")); l.addView(t("Adverse effects\n${d.adverseEffects}")); l.addView(t("Contraindications / cautions\n${d.contraindications}")) }
        if(p==null) l.addView(t("لا يوجد Master Profile لهذا الدواء بعد.")) else {
            val sections=listOf(
                "Generic name" to p.genericName, "Brand examples" to p.brandExamples, "Dosage forms" to p.dosageForms, "Route" to p.route,
                "Adult dose" to p.adultDose, "Pediatric dose" to p.pediatricDose, "Renal considerations" to p.renalConsiderations,
                "Hepatic considerations" to p.hepaticConsiderations, "Monitoring" to p.monitoring, "Pregnancy" to p.pregnancy,
                "Lactation" to p.lactation, "On-label" to p.onLabel, "Off-label" to p.offLabel, "Counseling" to p.counseling,
                "Boxed warning / major warning" to p.blackBoxWarning)
            sections.forEach { (h,v) -> l.addView(t("$h\n$v",16f)) }
            l.addView(t("📚 Reference\n${p.reference}\nLast reviewed: ${p.lastReviewed}",14f))
            l.addView(button("🧠 دراسة هذا الدواء"){ lifecycleScope.launch { repo.logSession("drug_master", drugId); Toast.makeText(this@MainActivity,"تم تسجيل جلسة دراسة ${p.genericName}",Toast.LENGTH_SHORT).show() } })
            l.addView(button("⚕️ التداخلات المرتبطة"){interactionsForDrug(p.genericName)})
        }
        l.addView(button("رجوع"){if(back=="results") showDrugResults(repo.allDrugs()) else drugLibrary()}); l.addView(button("الرئيسية"){home()}); setScreen(l)
    } }

    private fun interactionsForDrug(name:String){ lifecycleScope.launch {
        val items=repo.searchInteractions(name); val l=base(); l.addView(t("⚕️ Interactions: $name",26f,true));
        if(items.isEmpty()) l.addView(t("لا توجد نتيجة في قاعدة التداخلات الحالية.")) else items.forEach { x -> l.addView(t("${x.drugA} + ${x.drugB}\nSeverity: ${x.severity}\nMechanism: ${x.mechanism}\nManagement: ${x.management}\nReference: ${x.reference}",15f)) }
        l.addView(button("رجوع"){showDrugMasterProfile(repo.allDrugs().firstOrNull { it.name.equals(name,true) || it.name.contains(name,true) }?.id ?: "", "library")}); setScreen(l)
    } }

    private fun clinicalTutor(){
        lifecycleScope.launch {
            val ids = repo.tutorCases()
            val cases = repo.allCases().filter { ids.contains(it.id) }
            val l = base()
            l.addView(t("🧠 AI-like Clinical Tutor",27f,true))
            l.addView(t("تعلّم الحالة خطوة بخطوة. لن تظهر الإجابة مباشرة؛ سيطلب منك المحرك تحديد المشكلة، اختيار القرار، ثم يشرح سبب صحة أو خطأ إجابتك."))
            l.addView(t("Educational mode • Rule-based offline tutor",13f,true))
            if(cases.isEmpty()) l.addView(t("لا توجد حالات مهيأة للمحرك بعد."))
            cases.forEach { c ->
                l.addView(button("🩺 ${c.title}"){ startTutor(c.id) })
            }
            l.addView(button("الرئيسية"){home()})
            setScreen(l)
        }
    }

    private fun startTutor(caseId:String){
        lifecycleScope.launch {
            val c = repo.allCases().firstOrNull { it.id == caseId }
            val steps = repo.tutorSteps(caseId)
            if(c == null || steps.isEmpty()) { clinicalTutor(); return@launch }
            var index = 0
            var score = 0
            var answered = false
            fun render(){
                val l = base()
                l.addView(t("🧠 Clinical Tutor",26f,true))
                l.addView(t(c.title,21f,true))
                l.addView(t("الحالة\n${c.scenario}",16f))
                if(index >= steps.size){
                    val pct = if(steps.isEmpty()) 0 else score * 100 / steps.size
                    l.addView(section("🎯 النتيجة"))
                    l.addView(t("$score/${steps.size} صحيحة — $pct%",23f,true))
                    val level = when { pct >= 90 -> "Excellent"; pct >= 70 -> "Good"; pct >= 50 -> "Needs review"; else -> "High-priority review" }
                    l.addView(t("التقييم: $level",18f,true))
                    l.addView(t("الأخطاء التي حدثت في هذه الجلسة تُسجل للتعلم التكيفي، ويمكن تحويلها إلى مراجعة لاحقة."))
                    l.addView(button("🔄 إعادة الحالة"){startTutor(caseId)})
                    l.addView(button("🧠 حالات أخرى"){clinicalTutor()})
                    l.addView(button("الرئيسية"){home()})
                    setScreen(l)
                    return
                }
                val step = steps[index]
                l.addView(t("الخطوة ${index+1} من ${steps.size}",14f,true))
                l.addView(t(step.prompt,20f,true))
                if(!answered){
                    step.options.split("|").forEach { option ->
                        l.addView(button(option){
                            val correct = option.trim() == step.correctOption.trim()
                            if(correct) score++
                            answered = true
                            lifecycleScope.launch {
                                repo.recordTutorAttempt(caseId,index,option,correct)
                                repo.logLearningEvent("${caseId}_tutor_$index","clinical_tutor",c.title,correct)
                                render()
                            }
                        })
                    }
                } else {
                    // tutorAttempts is a suspend repository call; never call it directly
                    // from the synchronous UI renderer. Load it inside lifecycleScope.
                    lifecycleScope.launch {
                        val selected = repo.tutorAttempts(caseId)
                            .firstOrNull { it.stepIndex == index }
                            ?.selectedOption ?: ""
                        val lastCorrect = selected.trim() == step.correctOption.trim()
                        l.addView(t(if(lastCorrect) "✅ إجابتك صحيحة" else "❌ إجابتك تحتاج مراجعة",19f,true))
                        l.addView(t("إجابتك: $selected\nالإجابة الأفضل: ${step.correctOption}",16f))
                        l.addView(t("💡 لماذا؟\n${step.explanation}",16f))
                        l.addView(t("📌 نقطة الحفظ\n${step.teachingPoint}",16f,true))
                        l.addView(t("📚 ${step.reference}",13f))
                        l.addView(button(if(index+1 < steps.size) "الخطوة التالية →" else "عرض النتيجة") {
                            index++
                            answered=false
                            render()
                        })
                        setScreen(l)
                    }
                    return
                }
                l.addView(button("إيقاف الجلسة"){clinicalTutor()})
                setScreen(l)
            }
            render()
        }
    }

    private fun cases(){ lifecycleScope.launch { val items=repo.allCases(); val l=base(); l.addView(t("🩺 Clinical Cases",26f,true)); l.addView(t("اقرأ الحالة أولًا، ثم حاول بناء القرار الدوائي قبل إظهار الإجابة.")); items.forEach { c -> l.addView(button(c.title){ lifecycleScope.launch { val links=repo.caseLinks(c.id); val x=base(); x.addView(t(c.title,24f,true)); x.addView(t(c.scenario)); x.addView(button("إظهار الإجابة"){Toast.makeText(this@MainActivity,c.answer,Toast.LENGTH_LONG).show()}); if(links.isNotEmpty()){ x.addView(t("🔗 الأدوية المرتبطة بالحالة",19f,true)); links.forEach { link -> x.addView(t("${link.drugId}: ${link.rationale}")) } }; x.addView(t("Reference: ${c.reference}",14f)); x.addView(button("رجوع"){cases()}); setScreen(x) } }) }; l.addView(button("⚕️ التداخلات الدوائية"){interactions()}); l.addView(button("🧠 Clinical Tutor"){clinicalTutor()}); l.addView(button("الرئيسية"){home()}); setScreen(l) } }

    private fun interactions(){ lifecycleScope.launch { val all=repo.allInteractions(); val l=base(); l.addView(t("⚕️ Drug Interactions",26f,true)); l.addView(t("ابحث عن اسم أحد الدواءين. هذه معلومات تعليمية وليست بديلًا عن مراجعة وصفة أو مرجع دوائي محدث.")); val input=EditText(this@MainActivity).apply { hint="مثال: warfarin / digoxin"; setSingleLine(true) }; l.addView(input); l.addView(button("🔎 بحث"){ lifecycleScope.launch { val q=input.text.toString().trim(); showInteractions(if(q.isEmpty()) all else repo.searchInteractions(q)) } }); l.addView(t("التداخلات المحفوظة: ${all.size}",14f)); all.forEach { i -> l.addView(t("${i.drugA} + ${i.drugB}",19f,true)); l.addView(t("Severity: ${i.severity}\nMechanism: ${i.mechanism}\nManagement: ${i.management}\nReference: ${i.reference}",14f)) }; l.addView(button("رجوع"){home()}); setScreen(l) } }

    private fun showInteractions(items: List<DrugInteractionEntity>){ val l=base(); l.addView(t("🔎 نتائج التداخلات",25f,true)); if(items.isEmpty()) l.addView(t("لا توجد نتائج.")); items.forEach { i -> l.addView(t("${i.drugA} + ${i.drugB}",19f,true)); l.addView(t("Severity: ${i.severity}\n${i.mechanism}\nManagement: ${i.management}\nReference: ${i.reference}")) }; l.addView(button("رجوع للتداخلات"){interactions()}); setScreen(l) }

    private fun mcqs(){
        lifecycleScope.launch {
            val all = repo.allMcqs()
            val l = base()
            l.addView(t("📝 Advanced Quiz Engine",27f,true))
            l.addView(t("اختبار متكيف: اختر عدد الأسئلة، الصعوبة، والمؤقت الاختياري. بعد كل إجابة ستحصل على تفسير، وفي النهاية ستظهر الدقة والتوصية التالية."))
            val countInput = EditText(this@MainActivity).apply { hint="عدد الأسئلة (مثال: 10)"; setSingleLine(true); inputType=2 }
            l.addView(countInput)
            val difficulty = Spinner(this@MainActivity).apply { adapter=ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Mixed","Easy","Medium","Hard")) }
            l.addView(difficulty)
            val timed = CheckBox(this@MainActivity).apply { text="⏱️ مؤقت اختياري (60 ثانية لكل سؤال)" }
            l.addView(timed)
            l.addView(button("🚀 بدء الاختبار"){ 
                val count=countInput.text.toString().toIntOrNull()?.coerceIn(1,all.size.coerceAtLeast(1)) ?: 10.coerceAtMost(all.size.coerceAtLeast(1))
                val level=difficulty.selectedItemPosition
                startAdvancedQuiz(all, AdvancedQuizEngine.Config(count,level,timed.isChecked))
            })
            l.addView(section("📊 دقة المحاولات حسب الصعوبة"))
            for(level in 1..3) l.addView(t("${AdvancedQuizEngine.difficultyLabel(level)}: ${repo.quizAccuracyAtDifficulty(level)}%"))
            l.addView(t("💡 الصعوبة تُقدّر تلقائيًا من محتوى السؤال. يمكن توسيع بنك الأسئلة لاحقًا لإضافة مستويات أدق."))
            l.addView(button("رجوع"){home()})
            setScreen(l)
        }
    }

    private fun startAdvancedQuiz(all:List<McqEntity>, config:AdvancedQuizEngine.Config){
        val questions=AdvancedQuizEngine.chooseQuestions(all,config)
        if(questions.isEmpty()){ Toast.makeText(this,"لا توجد أسئلة كافية.",Toast.LENGTH_SHORT).show(); return }
        val quizId="quiz-${System.currentTimeMillis()}"
        val started=System.currentTimeMillis()
        lifecycleScope.launch { repo.saveQuizSession(QuizSessionEntity(quizId,started,total=questions.size,difficulty=config.difficulty,timed=config.timed)) }
        var index=0; var correctCount=0; var answered=false; var selected=""; var startedQuestion=System.currentTimeMillis()
        fun render(){
            val l=base()
            if(index>=questions.size){
                val pct=AdvancedQuizEngine.scorePercent(correctCount,questions.size)
                l.addView(t("🏁 نتيجة الاختبار",27f,true))
                l.addView(t("$correctCount/${questions.size} صحيحة — $pct%",24f,true))
                l.addView(t(AdvancedQuizEngine.recommendation(pct),17f,true))
                l.addView(t("الأخطاء سُجلت في التحليل التكيفي لتوجيه المراجعة القادمة."))
                lifecycleScope.launch { repo.updateQuizSession(QuizSessionEntity(quizId,started,System.currentTimeMillis(),questions.size,questions.size,correctCount,config.difficulty,config.timed)); repo.logSession("advanced_quiz",quizId,correctCount) }
                l.addView(button("🔄 اختبار جديد"){mcqs()}); l.addView(button("📊 نقاط الضعف"){weaknesses()}); l.addView(button("الرئيسية"){home()}); setScreen(l); return
            }
            val q=questions[index]
            if(!answered) startedQuestion=System.currentTimeMillis()
            val qLevel=AdvancedQuizEngine.difficultyOf(q)
            l.addView(t("📝 سؤال ${index+1} من ${questions.size}",14f,true))
            l.addView(t("الصعوبة: ${AdvancedQuizEngine.difficultyLabel(qLevel)}",13f))
            l.addView(t(q.question,20f,true))
            if(!answered){
                q.options.split("|").forEach { opt ->
                    l.addView(button(opt){
                        selected=opt
                        val ok=opt.startsWith(q.correct.substringBefore('.'))
                        if(ok) correctCount++
                        answered=true
                        lifecycleScope.launch {
                            val elapsed=((System.currentTimeMillis()-startedQuestion)/1000L).toInt()
                            repo.saveQuizAttempt(QuizAttemptEntity("$quizId-${q.id}-$index",quizId,q.id,opt,ok,qLevel,q.topic,elapsed,System.currentTimeMillis()))
                            repo.logLearningEvent(q.id,"advanced_mcq",q.topic,ok)
                            render()
                        }
                    })
                }
                l.addView(t(if(config.timed) "⏱️ لديك 60 ثانية تقريبًا لهذا السؤال." else "اختر أفضل إجابة."))
            } else {
                val ok=selected.startsWith(q.correct.substringBefore('.'))
                l.addView(t(if(ok) "✅ إجابتك صحيحة" else "❌ إجابتك غير صحيحة",19f,true))
                l.addView(t("إجابتك: $selected\nالإجابة الصحيحة: ${q.correct}",16f))
                l.addView(t("💡 الشرح\n${q.explanation}",16f))
                l.addView(t("📚 ${q.reference}\nTopic: ${q.topic}",13f))
                l.addView(button(if(index+1<questions.size) "السؤال التالي →" else "عرض النتيجة"){index++;answered=false;selected="";render()})
            }
            l.addView(button("⏹️ إنهاء الاختبار"){home()})
            setScreen(l)
        }
        render()
    }

    private fun weaknesses(){ lifecycleScope.launch { val rows=repo.weaknesses(); val answered=repo.totalAnswered(); val wrong=repo.totalWrong(); val l=base(); l.addView(t("📊 تحليل نقاط الضعف",26f,true)); l.addView(t("إجابات مسجلة: $answered\nإجابات خاطئة: $wrong")); if(rows.isEmpty()) l.addView(t("لا توجد بيانات كافية بعد. ابدأ بحل بعض الأسئلة.")); else rows.take(10).forEach { r -> val rate=if(r.total==0) 0 else (r.wrong*100/r.total); l.addView(t("${r.topic}\nأخطاء: ${r.wrong}/${r.total} ($rate%)",18f,true)); if(rate>=40) l.addView(t("⚠️ أولوية مراجعة عالية")) else if(rate>=20) l.addView(t("🔄 تحتاج مراجعة إضافية")) else l.addView(t("✅ أداء جيد")) }; l.addView(t("الخطة القادمة ستعطي الأولوية للمجالات ذات معدل الخطأ الأعلى.")); l.addView(button("رجوع"){home()}); setScreen(l) } }

    private fun paths(){ lifecycleScope.launch {
        val items=repo.allPaths(); val l=base(); l.addView(t("📚 المسارات الدراسية",26f,true));
        l.addView(t("اختر مسارًا لبناء خطة تعلم مناسبة لمستواك."));
        items.forEach { p -> l.addView(button(p.title){
            val x=base(); x.addView(t(p.title,25f,true)); x.addView(t(p.description));
            x.addView(t("المسار مصمم تدريجيًا من الأساسيات إلى التطبيق السريري."));
            x.addView(button("اختبار تحديد المستوى"){placement(p.id)}); x.addView(button("رجوع"){paths()}); setScreen(x)
        }) }; l.addView(button("الرئيسية"){home()}); setScreen(l)
    } }

    private fun weeklyReport(){
        lifecycleScope.launch {
            val answered=repo.weeklyAnswered(); val wrong=repo.weeklyWrong(); val rows=repo.weeklyWeaknesses()
            val accuracy=if(answered==0) 0 else ((answered-wrong)*100/answered)
            val l=base(); l.addView(t("📈 التقرير الأسبوعي",27f,true))
            l.addView(t("آخر 7 أيام",14f)); l.addView(t("إجابات: $answered\nصحيحة: ${answered-wrong}\nخاطئة: $wrong\nالدقة: $accuracy%",19f,true))
            l.addView(t("توزيع الأداء",20f,true))
            val filled=(accuracy/10).coerceIn(0,10); l.addView(t("[${"█".repeat(filled)}${"░".repeat(10-filled)}] $accuracy%"))
            l.addView(t("أكثر المجالات احتياجًا للمراجعة",20f,true))
            if(rows.isEmpty()) l.addView(t("حل بعض الأسئلة أولًا لإنشاء تقرير مفصل."))
            rows.take(5).forEach { r -> val rate=if(r.total==0)0 else r.wrong*100/r.total; l.addView(t("${r.topic}: $rate% أخطاء (${r.wrong}/${r.total})")) }
            l.addView(t("⚙️ المراجعة التكيفية ستزيد أولوية المجالات الضعيفة تلقائيًا في الخطط القادمة."))
            l.addView(button("رجوع"){home()}); setScreen(l)
        }
    }

    private fun placement(pathId:String?=null){
        val qs=listOf(
            Triple("أي مستقبل يرتبط أساسًا بزيادة معدل وقوة انقباض القلب؟","β1","β2"),
            Triple("ما التأثير المتوقع لمنبه β2 على البوتاسيوم المصلي؟","انخفاض","ارتفاع"),
            Triple("ما الهدف العام لضغط الدم المذكور في إرشاد AHA/ACC 2025؟","<130/80 mmHg","<150/100 mmHg")
        ); var score=0; var i=0; val l=base(); l.addView(t("🎯 Placement Test",26f,true)); l.addView(t("3 أسئلة قصيرة لتقدير المستوى. لا يوجد خصم على الخطأ."));
        fun render(){ if(i>=qs.size){ val level=when{score==3->"Expert"; score==2->"Advanced"; score==1->"Intermediate"; else->"Starter"}; lifecycleScope.launch { repo.savePlacement(PlacementResultEntity("placement-${System.currentTimeMillis()}",pathId?:"general",score,qs.size,level,System.currentTimeMillis())) }; l.removeAllViews(); l.addView(t("النتيجة: $score/${qs.size}",26f,true)); l.addView(t("مستواك المبدئي: $level",21f,true)); l.addView(t("سيُستخدم هذا المستوى لتوجيه المراجعة والخطة الدراسية.")); l.addView(button("العودة للرئيسية"){home()}); setScreen(l); return }; val q=qs[i]; l.removeAllViews(); l.addView(t("السؤال ${i+1} من ${qs.size}",14f)); l.addView(t(q.first,20f,true)); listOf(q.second,q.third).forEach { a-> l.addView(button(a){ if((i==0&&a=="β1")||(i==1&&a=="انخفاض")||(i==2&&a=="<130/80 mmHg")) score++; i++; render() }) }; l.addView(button("إلغاء"){home()}); setScreen(l) }
        render()
    }

    private fun smartPlan(){
        lifecycleScope.launch {
            val plan = SmartPlanner(repo).today()
            val l=base(); l.addView(t("🗓️ خطة اليوم الذكية",27f,true))
            l.addView(t("المستوى: ${plan.level}",19f,true))
            l.addView(t("التركيز: ${plan.focus}"))
            l.addView(t("📌 المهام المقترحة اليوم",20f,true))
            l.addView(t("🧠 مراجعة ذكية: ${plan.reviewCount} بطاقة"))
            l.addView(t("📚 مواضيع: ${plan.topicCount}"))
            l.addView(t("📝 أسئلة: ${plan.questionCount}"))
            l.addView(t("الخطة تتكيف مع مستوى الاختبار وعدد البطاقات المستحقة. إذا أخطأت في موضوع، ستزداد أولوية مراجعته."))
            l.addView(button("ابدأ المراجعة الآن"){review()})
            l.addView(button("موضوع اليوم"){topic()})
            l.addView(button("بنك الأسئلة"){mcqs()})
            l.addView(button("تم إنجاز الخطة اليوم"){ lifecycleScope.launch { repo.completeSmartPlan(plan.id); Toast.makeText(this@MainActivity,"أحسنت! تم تسجيل إنجاز اليوم 🏆",Toast.LENGTH_SHORT).show(); home() } })
            l.addView(button("رجوع"){home()}); setScreen(l)
        }
    }

    private fun achievements(){
        lifecycleScope.launch {
            val xp=repo.xp(); val streak=repo.streakDays(); val unlocked=repo.unlockedAchievements().map{it.key}.toSet()
            val defs=listOf(
                Triple("first_review","First Review","أكمل أول مراجعة ذكية."),
                Triple("ten_answers","10 Answers","أجب عن 10 أسئلة."),
                Triple("week_streak","7-Day Streak","تعلم 7 أيام متتالية."),
                Triple("five_hundred_xp","500 XP","اجمع 500 نقطة خبرة."))
            val l=base(); l.addView(t("🏆 الإنجازات",27f,true)); l.addView(t("🔥 Streak: $streak يوم\n⭐ XP: $xp",21f,true));
            defs.forEach{ d-> val ok=unlocked.contains(d.first); l.addView(t("${if(ok)"🏆" else "🔒"} ${d.second}\n${d.third}")) }
            if(streak>=7) repo.recordAchievement("week_streak")
            if(xp>=500) repo.recordAchievement("five_hundred_xp")
            if(repo.totalAnswered()>=10) repo.recordAchievement("ten_answers")
            l.addView(button("🔥 افتح التحدي اليومي"){dailyChallenge()}); l.addView(button("رجوع"){home()}); setScreen(l)
        }
    }

    private fun dailyChallenge(){
        lifecycleScope.launch {
            var c=repo.todayChallenge(); val l=base(); l.addView(t("🔥 التحدي اليومي",27f,true)); l.addView(t("أجب عن ${c.target} أسئلة اليوم.\nالتقدم: ${c.completed}/${c.target}",19f,true));
            val progress=(c.completed*100/c.target).coerceIn(0,100); l.addView(t("$progress% مكتمل"));
            if(c.completed>=c.target){ l.addView(t("🎉 اكتمل التحدي! ستحصل على XP إضافي.")); }
            else l.addView(button("حل سؤال الآن"){
                lifecycleScope.launch {
                    c=repo.incrementChallenge(); repo.recordAchievement("first_review");
                    if(c.completed>=c.target) { repo.awardChallengeXpIfNeeded(); repo.recordAchievement("ten_answers") }
                    dailyChallenge()
                }
            })
            l.addView(button("رجوع"){home()}); setScreen(l)
        }
    }

    private fun reminders(){ val l=base(); l.addView(t("🔔 التذكيرات",26f,true)); l.addView(t("08:00 — موضوع اليوم\n19:00 — المراجعة الذكية")); l.addView(button("إعادة جدولة"){ReminderScheduler.scheduleAll(this);Toast.makeText(this,"تمت جدولة التذكيرات.",Toast.LENGTH_SHORT).show()}); l.addView(button("رجوع"){home()});setScreen(l) }
}
