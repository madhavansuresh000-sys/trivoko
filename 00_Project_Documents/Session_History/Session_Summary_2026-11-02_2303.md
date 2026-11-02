# TriVoKo - Session Summary (2 Nov 2026, 23:03)

## Where we stopped
Phase 12 (Launch), **Step 3 Deploy - in progress**.

```
 Aiven MySQL  trivoko-mysql (Free, blr)        RUNNING
 Render       trivoko-api (Blueprint trivoko)  CREATED, build OK, start FAILS
              https://trivoko-api.onrender.com (name matches frontend/vercel.json)
 Vercel       not started
```

- main = 28c2776, GitHub CI green.
- Render auto-deploy is OFF (Blueprint made from the public repo URL) -> after a push use "Manual Deploy -> Deploy latest commit".

## Why Render fails (found)
Render has only 9 environment variables. The Blueprint form DROPPED all `sync: false` secrets:
DB_PASSWORD, DEMO_PASSWORD, CLOUDINARY_CLOUD_NAME / API_KEY / API_SECRET (Stripe ones are meant to stay empty).
Without DB_PASSWORD Spring sends the text `${DB_PASSWORD}` -> Aiven "Access denied for user 'avnadmin'".
Two saves through the Edit page did not stick (a row with an EMPTY value probably blocks the whole save).

## Next session - first things
1. **Remind MADHAVAN: he said he will give a NAME next time - ask him for it.**
2. MADHAVAN (passwords are his to type): Render -> trivoko-api -> Environment -> Edit -> "+ Add variable"
   - DB_PASSWORD = the Aiven password (copy icon on trivoko-mysql Overview)
   - DEMO_PASSWORD = his choice (temporary weak one for now - must be changed before the link is shared, Step 8)
   - Cloudinary rows later (never add a row with an empty value)
   - Save, rebuild, and deploy -> look for the green message.
3. The Aiven password was pasted in chat on 2 Nov -> advise Reset in Aiven, then put the NEW one in Render.
4. Claude: watch deploy -> /api/health UP -> Vercel (he signs up, root `frontend`) -> live test (browse, buy, return)
   -> fill Hosting_Values file -> Phase 12 steps 4-10.

## Still open
- Delete Aiven `pg-228c49d2` before 2 Dec 2026.
- Explain-backs Phases 9-11, Stripe dashboard check, k6 ceiling run.
