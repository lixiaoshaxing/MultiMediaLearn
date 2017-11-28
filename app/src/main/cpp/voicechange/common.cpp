#include "common.h"
#include "inc/fmod_errors.h"

void (*Common_Private_Error)(FMOD_RESULT, const char *, int);

void ERRCHECK_fn(FMOD_RESULT result, const char *file, int line)
{
    if (result != FMOD_OK)
    {
        if (Common_Private_Error)
        {
            Common_Private_Error(result, file, line);
        }
        Common_Fatal("%s(%d): FMOD error %d - %s", file, line, result, FMOD_ErrorString(result));
    }
}

void Common_Format(char *buffer, int bufferSize, const char *formatString, ...)
{
    va_list args;
    va_start(args, formatString);
    Common_vsnprintf(buffer, bufferSize, formatString, args);
    va_end(args);
    buffer[bufferSize-1] = '\0';
}

void Common_Fatal(const char *format, ...)
{
    char error[1024];

    va_list args;
    va_start(args, format);
    Common_vsnprintf(error, 1024, format, args);
    va_end(args);
    error[1023] = '\0';

    do
    {
        Common_Draw("A fatal error has occurred...");
        Common_Draw("");
        Common_Draw("%s", error);
        Common_Draw("");
        Common_Draw("Press %s to quit", Common_BtnStr(BTN_QUIT));

        Common_Update();
        Common_Sleep(50);
    } while (!Common_BtnPress(BTN_QUIT));

    Common_Exit(0);
}

void Common_Draw(const char *format, ...)
{
    char string[1024];
    char *stringPtr = string;

    va_list args;
    va_start(args, format);
    Common_vsnprintf(string, 1024, format, args);
    va_end(args);
    string[1023] = '\0';

    unsigned int length = (unsigned int)strlen(string);

    do
    {
        bool consumeNewLine = false;
        unsigned int copyLength = length;

        // Search for new line characters
        char *newLinePtr = strchr(stringPtr, '\n');
        if (newLinePtr)
        {
            consumeNewLine = true;
            copyLength = (unsigned int)(newLinePtr - stringPtr);
        }

        if (copyLength > NUM_COLUMNS)
        {
            // Hard wrap by default
            copyLength = NUM_COLUMNS;

            // Loop for a soft wrap
            for (int i = NUM_COLUMNS - 1; i >= 0; i--)
            {
                if (stringPtr[i] == ' ')
                {
                    copyLength = i + 1;
                    break;
                }
            }
        }

        // Null terminate the sub string temporarily by swapping out a char
        char tempChar = stringPtr[copyLength];
        stringPtr[copyLength] = 0;
        Common_DrawText(stringPtr);
        stringPtr[copyLength] = tempChar;

        copyLength += (consumeNewLine ? 1 : 0);
        length -= copyLength;
        stringPtr += copyLength;
    } while (length > 0);
}

