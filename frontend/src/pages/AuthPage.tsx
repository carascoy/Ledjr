import { useState } from 'react'
import '../styles/AuthPage.css'

type Mode = "signin" | "signup";

function AuthPage(){
    const [mode, setMode] = useState<Mode>('signin')

    const isSignUp = mode === 'signup'

    return (
        <div className="auth-page">
            <h1 className="auth-title">Ledjr</h1>
            <div className="auth-card">

                <div className="auth-toggle">
                    <button type ="button" className={mode === 'signin' ? 'toggle-active' : 'toggle-inactive'} onClick={() => setMode("signin")}>
                        Sign In
                    </button>
                    <button type ="button" className={mode === 'signup' ? 'toggle-active' : 'toggle-inactive'} onClick={() => setMode("signup")}>
                        Sign Up
                    </button>
                </div>

                <div className={`collapsible ${isSignUp ? 'expanded' : ''}`}>
                    <div className="collapsible-inner">
                        <input className="auth-input" placeholder="First Name" />
                        <input className="auth-input" placeholder="Last Name" />
                    </div>
                </div>

                <input className="auth-input" placeholder="Email"/>
                <input className="auth-input" type="password" placeholder="Password"/>

                 <div className={`collapsible ${isSignUp ? 'expanded' : ''}`}>
                    <div className="collapsible-inner password-hints">
                        <p>At least 8 characters.</p>
                        <p>At least 1 number.</p>
                        <p>At least 1 special character.</p>
                    </div>
                </div>

                <button type="button" className="auth-submit">{isSignUp ? 'Sign Up' : 'Sign In'}</button>

            </div>
        </div>
    )
}

export default AuthPage
