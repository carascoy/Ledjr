import { useEffect, useState } from 'react'
import '../styles/AuthPage.css'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import Splash from '../components/Splash'
import { useUser } from '../context/UserContext'

type Mode = "signin" | "signup";

type Error = "account-not-found" | "missing" | "email-registered" | "too-many" | "none";

function AuthPage(){
    const navigate = useNavigate()
    const { user, setUser, checking } = useUser()

    const [searchParams] = useSearchParams()
    const [mode, setMode] = useState<Mode>(searchParams.get('mode') === 'signup' ? 'signup' : 'signin')
    const [error, setError] = useState<Error>('none');
    // the server's "Try again in 12 minutes." after too many wrong passwords
    const [tooManyMessage, setTooManyMessage] = useState('')
    // brand new account -> the optional setup instead of the dashboard
    const [justRegistered, setJustRegistered] = useState(false)
    const [startingDemo, setStartingDemo] = useState(false)
    const [demoError, setDemoError] = useState('')

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')

    const isSignUp = mode === 'signup'

    useEffect(() => {
        document.title = `${isSignUp ? 'Sign up' : 'Sign in'} · Ledjr`
    }, [isSignUp])
    const accountNotFound = error === 'account-not-found';
    const missing = error === 'missing';
    const emailRegistered = error === "email-registered";
    const tooMany = error === "too-many";

    const hasEightChars = password.length >= 8 
    const hasNumber = /[0-9]/.test(password)
    const hasSpecialChar = /[^A-Za-z0-9]/.test(password)
    const isPasswordValid = hasEightChars && hasNumber && hasSpecialChar

    // sign in is never disabled: browsers hide autofilled values from the page until you click something,
    // so the boxes can look filled while email/password are still ''. handleSubmit reads the form instead
    const canSubmit = isSignUp ? isPasswordValid : true

    const isCharValid = hasEightChars;
    const isNumValid = hasNumber;
    const isSpecialCharValid = hasSpecialChar;


    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault()

        if(!isSignUp){
            const form = new FormData(e.currentTarget as HTMLFormElement)
            const signinEmail = String(form.get('email') ?? '').trim()
            const signinPassword = String(form.get('password') ?? '')
            setEmail(signinEmail)
            setPassword(signinPassword)
            if (!signinEmail || !signinPassword) {
                setError("missing")
                return
            }

            const response = await api('/signin', {
                method: 'POST',
                body: JSON.stringify({email: signinEmail, password: signinPassword}),
            })

            if(response.ok){
                const data = await response.json()
                setUser(data)
                navigate('/dashboard')
                console.log("User successfully logged in.")
            }else{
                const message = await response.text()
                if (response.status === 429) {
                    setTooManyMessage(message)
                    setError("too-many")
                } else {
                    setError("account-not-found")
                }
                console.log('Signin failed:', message)

            }

        }else{

            const response = await api('/register', {
                method: 'POST',
                body: JSON.stringify({ email, password, firstName, lastName }),
            })

            if (response.ok) {
                const data = await response.json()
                // new accounts get the optional setup first (skippable, see SetupPage).
                // the `if (user)` redirect above does the navigating once user is set
                setJustRegistered(true)
                setUser(data)
                console.log('Account created')
            } else {
                const message = await response.text()
                console.log('Signup failed:', message)
                setError("email-registered")
            }
        }

    }

   

    async function startDemo() {
        setStartingDemo(true)
        setDemoError('')
        try {
            const response = await api('/demo', { method: 'POST' })
            if (response.ok) {
                setUser(await response.json())
                navigate('/dashboard')
                return
            }
            setDemoError(response.status === 429 ? await response.text() : "Couldn't start the demo. Try again in a minute.")
        } catch {
            setDemoError("Couldn't reach the server. Try again in a minute.")
        }
        setStartingDemo(false)
    }

    // still asking the backend if the session is good. logo instead of the form,
    // otherwise the sign in form flashes for a split second before the redirect
    if (checking) {
        return <Splash />
    }
    // already logged in (session cookie still good) -> skip the sign in page
    if (user) {
        return <Navigate to={justRegistered ? "/setup" : "/dashboard"} replace />
    }

    return (
        <div className="auth-page">
            <h1 className="auth-title">Ledjr</h1>
            <form className="auth-card" onSubmit={handleSubmit}>

                <div className="auth-toggle">
                    <button type ="button" className={mode === 'signin' ? 'toggle-active' : 'toggle-inactive'} onClick={() => { setMode("signin"); setError("none") }}>
                        Sign In
                    </button> 
                    <button type ="button" className={mode === 'signup' ? 'toggle-active' : 'toggle-inactive'} onClick={() => {setMode("signup"); setError("none")}}>
                        Sign Up
                    </button>
                </div>

                <div className={`collapsible ${isSignUp ? 'expanded' : ''}`}>
                    <div className="collapsible-inner">
                        <input className="auth-input" name="firstName" autoComplete="given-name" placeholder="First Name" value={firstName} onChange={(e) => setFirstName(e.target.value)}/>
                        <input className="auth-input" name="lastName" autoComplete="family-name" placeholder="Last Name" value={lastName} onChange={(e) => setLastName(e.target.value)}/>
                    </div>
                </div>

                <input className="auth-input" name="email" inputMode="email" autoComplete="email" autoCapitalize="none" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)}/>
                <input className="auth-input" type="password" name="password" autoComplete={isSignUp ? "new-password" : "current-password"} placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)}/>

                <div className={`collapsible ${accountNotFound ? 'expanded' : ''}`}>
                    <div className="collapsible-inner error-text">
                        <p>Incorrect email or password.</p>
                    </div>
                </div>

                <div className={`collapsible ${missing ? 'expanded' : ''}`}>
                    <div className="collapsible-inner error-text">
                        <p>Enter your email and password.</p>
                    </div>
                </div>

                <div className={`collapsible ${tooMany ? 'expanded' : ''}`}>
                    <div className="collapsible-inner error-text">
                        <p>{tooManyMessage}</p>
                    </div>
                </div>

                 <div className={`collapsible ${isSignUp ? 'expanded' : ''}`}>
                    <div className="collapsible-inner password-hints">
                        <p className={isCharValid ? 'correct' : 'wrong'}>At least 8 characters.</p>
                        <p className={isNumValid ? 'correct' : 'wrong'}>At least 1 number.</p>
                        <p className={isSpecialCharValid ? 'correct' : 'wrong'}>At least 1 special character.</p>
                    </div>

                    <div className={`collapsible ${emailRegistered ? 'expanded' : ''}`}>
                        <div className="collapsible-inner error-text">
                        <p>Email is already registered to another account.</p>
                    </div>
                </div>

                </div>

                <button type="submit" disabled={!canSubmit} className='auth-submit'>{isSignUp ? 'Sign Up' : 'Sign In'}</button>

                {!isSignUp && (
                    <Link to="/forgot-password" className="auth-link auth-forgot">Forgot your password?</Link>
                )}
                {isSignUp && (
                    <p className="auth-consent">
                        By creating an account you agree to the <Link to="/terms">Terms</Link> and <Link to="/privacy">Privacy Policy</Link>.
                    </p>
                )}

            </form>

            <div className="auth-demo">
                <p className="auth-demo-text">Just looking around?</p>
                <button type="button" className="auth-demo-btn" onClick={startDemo} disabled={startingDemo}>
                    {startingDemo ? 'Setting up the demo…' : 'Try the demo'}
                </button>
                <p className="auth-demo-note">No sign up. Made-up data, gone after 24 hours.</p>
                {demoError && <p className="error-text auth-demo-error">{demoError}</p>}
            </div>

            <nav className="auth-legal" aria-label="Legal">
                <Link to="/privacy">Privacy</Link>
                <span aria-hidden="true">·</span>
                <Link to="/terms">Terms</Link>
            </nav>
        </div>
    )
}

export default AuthPage
