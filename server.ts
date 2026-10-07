import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { GoogleGenAI, Type } from '@google/genai';
import { createServer as createViteServer } from 'vite';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

const apiKey = process.env.GEMINI_API_KEY;
const ai = new GoogleGenAI({
  apiKey,
  httpOptions: {
    headers: {
      'User-Agent': 'aistudio-build',
    },
  },
});

// Endpoint 1: AI Syllabus Image / Text Extraction
app.post('/api/gemini/extract-syllabus', async (req, res) => {
  try {
    const { imageBase64, mimeType, rawText, defaultCourseName } = req.body;

    const parts: any[] = [];
    if (imageBase64) {
      parts.push({
        inlineData: {
          mimeType: mimeType || 'image/jpeg',
          data: imageBase64,
        },
      });
    }

    const instruction = `
You are an expert academic curriculum parser. 
Carefully analyze the provided university syllabus ${imageBase64 ? 'image/document' : 'text'}.
Identify:
1. Course / Subject Name (and course code if visible, e.g. "CS 301")
2. Units / Modules / Chapters with titles (e.g. "Unit 1: Foundations", "Module 2: Advanced Data Structures")
3. All topics and subtopics listed under each chapter in exact sequence.
4. Mark importance as 'NORMAL', 'IMPORTANT', or 'VERY_IMPORTANT' if the syllabus indicates emphasis (e.g. weightage, repeated stars, marks).

Format the output strictly as a JSON object adhering to this schema:
{
  "courseName": "string",
  "courseCode": "string",
  "chapters": [
    {
      "id": "chap-1",
      "title": "string",
      "topics": [
        {
          "id": "top-1",
          "name": "string",
          "description": "string",
          "importance": "NORMAL" | "IMPORTANT" | "VERY_IMPORTANT"
        }
      ]
    }
  ]
}

If any topic text is unclear or slightly blurry, make your best approximation and do not omit topics.
${rawText ? `Raw text input:\n${rawText}` : ''}
${defaultCourseName ? `Fallback course name if not specified: ${defaultCourseName}` : ''}
`;

    parts.push({ text: instruction });

    const response = await ai.models.generateContent({
      model: 'gemini-3.8-flash',
      contents: { parts },
      config: {
        responseMimeType: 'application/json',
      },
    });

    const text = response.text || '{}';
    const parsed = JSON.parse(text);
    return res.json({ success: true, data: parsed });
  } catch (error: any) {
    console.error('Error extracting syllabus:', error);
    return res.status(500).json({
      success: false,
      message: error?.message || 'Failed to extract syllabus from image',
    });
  }
});

// Endpoint 2: AI Revision Plan Generation
app.post('/api/gemini/generate-revision-plan', async (req, res) => {
  try {
    const { subjects, topics, pyqs, exams, completedRevisions, todayStr } = req.body;

    const academicContextPrompt = `
You are a university academic strategist and memory retention coach.
The student has the following real academic state:
- Today's Date: ${todayStr}
- Enrolled Subjects (${subjects?.length || 0}):
${JSON.stringify(subjects || [], null, 2)}

- Syllabus Topics (${topics?.length || 0}):
${JSON.stringify(topics || [], null, 2)}

- Past Exam Questions / PYQs (${pyqs?.length || 0}):
${JSON.stringify(pyqs || [], null, 2)}

- Upcoming Exam Cycles (${exams?.length || 0}):
${JSON.stringify(exams || [], null, 2)}

- Previous Revisions Completed:
${JSON.stringify(completedRevisions || [], null, 2)}

Your task:
Analyze this real student data and generate a prioritized, intelligent, personalized AI Revision Plan.
DO NOT create a rigid minute-by-minute timetable. The student controls their daily routine.
Instead, generate specific high-value actionable revision recommendations.

Priority Rules:
- HIGH PRIORITY:
  * Repeated PYQ topics
  * Topics with WEAK understanding level
  * High-mark exam topics
  * Uncompleted topics with upcoming exams in < 14 days
  * Overdue spaced revisions
- MEDIUM PRIORITY:
  * Completed topics not revised recently
  * Medium-frequency PYQs
  * Topics with OKAY understanding
- LOW PRIORITY:
  * STRONG topics
  * Topics revised within the last 48 hours
  * Low-frequency / minor topics

Return a strict JSON object with:
{
  "recommendations": [
    {
      "id": "rec-1",
      "subjectId": number,
      "subjectName": "string",
      "topicId": number or null,
      "topicName": "string",
      "priority": "HIGH" | "MEDIUM" | "LOW",
      "reason": "Clear explanation connecting PYQs, exam proximity, or weak understanding",
      "suggestedDate": "YYYY-MM-DD",
      "suggestedWindow": "e.g. Next 2 days / Urgent before exam / This weekend",
      "actionType": "REVISE_TOPIC" | "PRACTICE_PYQS" | "DEEP_DIVE_WEAK" | "QUICK_FORMULA_CHECK",
      "isCompleted": false
    }
  ],
  "overallStrategySummary": "2-3 sentences summarizing the key academic focus for this exam season"
}
`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.8-flash',
      contents: academicContextPrompt,
      config: {
        responseMimeType: 'application/json',
      },
    });

    const text = response.text || '{}';
    const parsed = JSON.parse(text);
    return res.json({ success: true, data: parsed });
  } catch (error: any) {
    console.error('Error generating revision plan:', error);
    return res.status(500).json({
      success: false,
      message: error?.message || 'Failed to generate AI revision plan',
    });
  }
});

// Endpoint 3: AI Study Assistant Chat (with context, images, voice & Google Search)
app.post('/api/gemini/chat', async (req, res) => {
  try {
    const { message, history = [], userContext, imageBase64, mimeType, useSearch } = req.body;

    const systemInstruction = `
You are the AI Study Assistant built into Semester Study OS.
You are helping a university student with their courses, syllabus, revisions, and exam preparation.

STUDENT'S REAL ACADEMIC CONTEXT:
${JSON.stringify(userContext || {}, null, 2)}

Instructions:
1. PRIORITIZE THE STUDENT'S REAL DATA: Always refer to their actual subjects, syllabus topics, upcoming exam dates, and PYQs.
2. Never invent courses or topics that are not in their context. If they ask about a course or topic they haven't added yet, clearly let them know they can add it via "+ Add Subject" or "Upload Syllabus".
3. If the student uploads an image (lecture note, textbook page, handwritten problem, or diagram), analyze it thoroughly, explain the solution or concept step-by-step, and connect it to their syllabus if relevant.
4. When answering questions like "What should I study today?" or "Which topic should I revise first?", analyze their upcoming exam dates, weak topics, and PYQ frequency to give concrete advice.
5. Keep explanations structured, clear, and encouraging. Use Markdown formatting.
`;

    const contents: any[] = [];

    // Prior history if provided
    for (const h of history) {
      contents.push({
        role: h.role === 'user' ? 'user' : 'model',
        parts: [{ text: h.text }],
      });
    }

    const currentParts: any[] = [];
    if (imageBase64) {
      currentParts.push({
        inlineData: {
          mimeType: mimeType || 'image/jpeg',
          data: imageBase64,
        },
      });
    }
    currentParts.push({ text: message || 'Please analyze this.' });

    contents.push({
      role: 'user',
      parts: currentParts,
    });

    const config: any = {
      systemInstruction,
    };

    if (useSearch) {
      config.tools = [{ googleSearch: {} }];
    }

    const response = await ai.models.generateContent({
      model: 'gemini-3.8-flash',
      contents,
      config,
    });

    const replyText = response.text || '';

    // Extract search grounding metadata if available
    const groundingChunks =
      response.candidates?.[0]?.groundingMetadata?.groundingChunks || [];
    const webSearchQueries =
      response.candidates?.[0]?.groundingMetadata?.webSearchQueries || [];

    const sources = groundingChunks
      .filter((c: any) => c.web?.uri && c.web?.title)
      .map((c: any) => ({
        title: c.web.title,
        url: c.web.uri,
      }));

    return res.json({
      success: true,
      text: replyText,
      sources,
      webSearchQueries,
    });
  } catch (error: any) {
    console.error('Error in study assistant chat:', error);
    return res.status(500).json({
      success: false,
      message: error?.message || 'Failed to get AI response',
    });
  }
});

// Setup Vite middleware in dev or static files in production
async function startServer() {
  const isProd = process.env.NODE_ENV === 'production';
  if (!isProd) {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    app.use(express.static(path.resolve(__dirname, 'dist')));
    app.get('*', (_req, res) => {
      res.sendFile(path.resolve(__dirname, 'dist', 'index.html'));
    });
  }

  const PORT = 3000;
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Semester Study OS Server running at http://0.0.0.0:${PORT}`);
  });
}

startServer();
