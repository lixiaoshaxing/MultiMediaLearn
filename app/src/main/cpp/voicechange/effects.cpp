/*==============================================================================
Effects Example
Copyright (c), Firelight Technologies Pty, Ltd 2004-2017.

This example shows how to apply some of the built in software effects to sounds
by applying them to the master channel group. All software sounds played here
would be filtered in the same way. To filter per channel, and not have other
channels affected, simply apply the same functions to the FMOD::Channel instead
of the FMOD::ChannelGroup.
==============================================================================*/
#include "inc/fmod.hpp"
#include "common.h"

int FMOD_Main()
{
    FMOD::System       *system        = 0;
    FMOD::Sound        *sound         = 0;
    FMOD::Channel      *channel       = 0;
    FMOD::ChannelGroup *mastergroup   = 0; 
    FMOD::DSP          *dsplowpass    = 0;
    FMOD::DSP          *dsphighpass   = 0;
    FMOD::DSP          *dspecho       = 0;
    FMOD::DSP          *dspflange     = 0;
    FMOD_RESULT         result;
    unsigned int        version;
    void               *extradriverdata = 0;

    Common_Init(&extradriverdata);

    /*
        Create a System object and initialize
    */
    result = FMOD::System_Create(&system);
    ERRCHECK(result);

    result = system->getVersion(&version);
    ERRCHECK(result);

    if (version < FMOD_VERSION)
    {
        Common_Fatal("FMOD lib version %08x doesn't match header version %08x", version, FMOD_VERSION);
    }

    result = system->init(32, FMOD_INIT_NORMAL, extradriverdata);
    ERRCHECK(result);

    result = system->getMasterChannelGroup(&mastergroup);
    ERRCHECK(result);

    result = system->createSound(Common_MediaPath("drumloop.wav"), FMOD_DEFAULT, 0, &sound);
    ERRCHECK(result);

    result = system->playSound(sound, 0, false, &channel);
    ERRCHECK(result);

    /*
        Create some effects to play with
    */
    result = system->createDSPByType(FMOD_DSP_TYPE_LOWPASS, &dsplowpass);
    ERRCHECK(result);
    result = system->createDSPByType(FMOD_DSP_TYPE_HIGHPASS, &dsphighpass);
    ERRCHECK(result);
    result = system->createDSPByType(FMOD_DSP_TYPE_ECHO, &dspecho);
    ERRCHECK(result);
    result = system->createDSPByType(FMOD_DSP_TYPE_FLANGE, &dspflange);
    ERRCHECK(result);

    /*
        Add them to the master channel group.  Each time an effect is added (to position 0) it pushes the others down the list.
    */
    result = mastergroup->addDSP(0, dsplowpass);
    ERRCHECK(result);
    result = mastergroup->addDSP(0, dsphighpass);
    ERRCHECK(result);
    result = mastergroup->addDSP(0, dspecho);
    ERRCHECK(result);
    result = mastergroup->addDSP(0, dspflange);
    ERRCHECK(result);

    /*
        By default, bypass all effects.  This means let the original signal go through without processing.
        It will sound 'dry' until effects are enabled by the user.
    */
    result = dsplowpass->setBypass(true);
    ERRCHECK(result);
    result = dsphighpass->setBypass(true);
    ERRCHECK(result);
    result = dspecho->setBypass(true);
    ERRCHECK(result);
    result = dspflange->setBypass(true);
    ERRCHECK(result);

    /*
        Main loop
    */
    do
    {
        Common_Update();

        if (Common_BtnPress(BTN_MORE))
        {
            bool paused;

            result = channel->getPaused(&paused);
            ERRCHECK(result);

            paused = !paused;

            result = channel->setPaused(paused);
            ERRCHECK(result);
        }

        if (Common_BtnPress(BTN_ACTION1))
        {
            bool bypass;

            result = dsplowpass->getBypass(&bypass);
            ERRCHECK(result);

            bypass = !bypass;

            result = dsplowpass->setBypass(bypass);
            ERRCHECK(result);
        }

        if (Common_BtnPress(BTN_ACTION2))
        {
            bool bypass;

            result = dsphighpass->getBypass(&bypass);
            ERRCHECK(result);

            bypass = !bypass;

            result = dsphighpass->setBypass(bypass);
            ERRCHECK(result);
        }

        if (Common_BtnPress(BTN_ACTION3))
        {
            bool bypass;

            result = dspecho->getBypass(&bypass);
            ERRCHECK(result);

            bypass = !bypass;

            result = dspecho->setBypass(bypass);
            ERRCHECK(result);
        }

        if (Common_BtnPress(BTN_ACTION4))
        {
            bool bypass;

            result = dspflange->getBypass(&bypass);
            ERRCHECK(result);

            bypass = !bypass;

            result = dspflange->setBypass(bypass);
            ERRCHECK(result);
        }

        result = system->update();
        ERRCHECK(result);

        {
            bool paused = 0;
            bool dsplowpass_bypass;
            bool dsphighpass_bypass;
            bool dspecho_bypass;
            bool dspflange_bypass;

            dsplowpass   ->getBypass(&dsplowpass_bypass);
            dsphighpass  ->getBypass(&dsphighpass_bypass);
            dspecho      ->getBypass(&dspecho_bypass);
            dspflange    ->getBypass(&dspflange_bypass);

            if (channel)
            {
                result = channel->getPaused(&paused);
                if ((result != FMOD_OK) && (result != FMOD_ERR_INVALID_HANDLE) && (result != FMOD_ERR_CHANNEL_STOLEN))
                {
                    ERRCHECK(result);
                }
            }

            Common_Draw("==================================================");
            Common_Draw("Effects Example.");
            Common_Draw("Copyright (c) Firelight Technologies 2004-2017.");
            Common_Draw("==================================================");
            Common_Draw("");
            Common_Draw("Press %s to pause/unpause sound", Common_BtnStr(BTN_MORE));
            Common_Draw("Press %s to toggle dsplowpass effect", Common_BtnStr(BTN_ACTION1));
            Common_Draw("Press %s to toggle dsphighpass effect", Common_BtnStr(BTN_ACTION2));
            Common_Draw("Press %s to toggle dspecho effect", Common_BtnStr(BTN_ACTION3));
            Common_Draw("Press %s to toggle dspflange effect", Common_BtnStr(BTN_ACTION4));
            Common_Draw("Press %s to quit", Common_BtnStr(BTN_QUIT));
            Common_Draw("");
            Common_Draw("%s : lowpass[%c] highpass[%c] echo[%c] flange[%c]", 
                    paused              ? "Paused " : "Playing",
                    dsplowpass_bypass   ? ' ' : 'x',
                    dsphighpass_bypass  ? ' ' : 'x',
                    dspecho_bypass      ? ' ' : 'x',
                    dspflange_bypass    ? ' ' : 'x');
        }

        Common_Sleep(50);
    } while (!Common_BtnPress(BTN_QUIT));

    /*
        Shut down
    */
    result = mastergroup->removeDSP(dsplowpass);
    ERRCHECK(result);
    result = mastergroup->removeDSP(dsphighpass);
    ERRCHECK(result);
    result = mastergroup->removeDSP(dspecho);
    ERRCHECK(result);
    result = mastergroup->removeDSP(dspflange);
    ERRCHECK(result);
    
    result = dsplowpass->release();
    ERRCHECK(result);
    result = dsphighpass->release();
    ERRCHECK(result);
    result = dspecho->release();
    ERRCHECK(result);
    result = dspflange->release();
    ERRCHECK(result);

    result = sound->release();
    ERRCHECK(result);
    result = system->close();
    ERRCHECK(result);
    result = system->release();
    ERRCHECK(result);

    Common_Close();

    return 0;
}
