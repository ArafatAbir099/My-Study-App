export interface ParsedUnit {
  title: string;
  topics: { name: string; description: string }[];
}

export interface ParsedCourse {
  courseName: string;
  courseCode: string;
  units: ParsedUnit[];
}

export function parseSyllabusContent(rawText: string, defaultCourseName = 'Course'): ParsedCourse[] {
  const lines = rawText
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l.length > 0);

  const units: ParsedUnit[] = [];
  let currentUnit: ParsedUnit = {
    title: 'Module 1: Foundations',
    topics: [],
  };

  const moduleRegex = /^(?:module|chapter|unit|section|part|week)\s*(\d+|[ivxlcdm]+)[:\s-]*(.*)/i;

  for (const line of lines) {
    const modMatch = line.match(moduleRegex);
    if (modMatch) {
      if (currentUnit.topics.length > 0) {
        units.push(currentUnit);
      }
      const titleNum = modMatch[1];
      const rest = modMatch[2]?.trim() || `Unit ${titleNum}`;
      currentUnit = {
        title: `Module ${titleNum}: ${rest}`,
        topics: [],
      };
      continue;
    }

    // Check if line looks like a bullet or numbered topic
    const cleanedLine = line
      .replace(/^[-*•–—]\s*/, '')
      .replace(/^\d+[\.\)]\s*/, '')
      .trim();

    if (cleanedLine.length > 2) {
      // Split subtopics if separated by commas or semicolons
      if (cleanedLine.includes(';') || (cleanedLine.includes(',') && cleanedLine.length > 60)) {
        const parts = cleanedLine.split(/[;,]/).map((p) => p.trim()).filter((p) => p.length > 2);
        for (const part of parts) {
          currentUnit.topics.push({
            name: part,
            description: '',
          });
        }
      } else {
        currentUnit.topics.push({
          name: cleanedLine,
          description: '',
        });
      }
    }
  }

  if (currentUnit.topics.length > 0) {
    units.push(currentUnit);
  }

  // If no modules were detected, group topics in chunks of 5 into modules
  if (units.length === 1 && units[0].topics.length > 7) {
    const allTopics = units[0].topics;
    const chunkedUnits: ParsedUnit[] = [];
    const chunkSize = 5;
    for (let i = 0; i < allTopics.length; i += chunkSize) {
      chunkedUnits.push({
        title: `Module ${Math.floor(i / chunkSize) + 1}`,
        topics: allTopics.slice(i, i + chunkSize),
      });
    }
    return [
      {
        courseName: defaultCourseName,
        courseCode: '',
        units: chunkedUnits,
      },
    ];
  }

  return [
    {
      courseName: defaultCourseName,
      courseCode: '',
      units: units.length > 0 ? units : [
        {
          title: 'Module 1: Core Topics',
          topics: [{ name: 'Introduction to Course', description: '' }],
        },
      ],
    },
  ];
}
