import { useState } from 'react'
import '../styles/AuthPage.css'
import { Navigate, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import Splash from '../components/Splash'
import { useUser } from '../context/UserContext'

type Mode = "signin" | "signup";

type Error = "account-not-found" | "email-registered" | "none";

function AuthPage(){
    const navigate = useNavigate()
    const { user, setUser, checking } = useUser()

    const [mode, setMode] = useState<Mode>('signin')
    const [error, setError] = useState<Error>('none');
    // brand new account -> the optional setup instead of the dashboard
    const [justRegistered, setJustRegistered] = useState(false)

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')

    const isSignUp = mode === 'signup'
    const accountNotFound = error === 'account-not-found';
    const emailRegistered = error === "email-registered";

    const hasEightChars = password.length >= 8 
    const hasNumber = /[0-9]/.test(password)
    const hasSpecialChar = /[^A-Za-z0-9]/.test(password)
    const isPasswordValid = hasEightChars && hasNumber && hasSpecialChar

    // sign up has to meet the password rules, sign in just needs both boxes filled
    // (older accounts might not meet newer rules, and the server checks the password anyway)
    const canSubmit = isSignUp ? isPasswordValid : email.trim() !== '' && password !== ''

    const isCharValid = hasEightChars;
    const isNumValid = hasNumber;
    const isSpecialCharValid = hasSpecialChar;


    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault()

        if(!isSignUp){
            const response = await api('/signin', {
                method: 'POST',
                body: JSON.stringify({email, password}),
            })

            if(response.ok){
                const data = await response.json()
                setUser(data)
                navigate('/dashboard')
                console.log("User successfully logged in.")
            }else{
                const message = await response.text()
                setError("account-not-found")
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
                        <input className="auth-input" placeholder="First Name" value={firstName} onChange={(e) => setFirstName(e.target.value)}/>
                        <input className="auth-input" placeholder="Last Name" value={lastName} onChange={(e) => setLastName(e.target.value)}/>
                    </div>
                </div>

                <input className="auth-input" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)}/>
                <input className="auth-input" type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)}/>

                <div className={`collapsible ${accountNotFound ? 'expanded' : ''}`}>
                    <div className="collapsible-inner error-text">
                        <p>Incorrect email or password.</p>
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

            </form>
        </div>
    )
}

export default AuthPage
