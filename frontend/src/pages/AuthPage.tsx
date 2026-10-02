import { useState } from 'react'
import '../styles/AuthPage.css'
import { useNavigate } from 'react-router-dom' 

type Mode = "signin" | "signup";

function AuthPage(){
     const navigate = useNavigate()

    const [mode, setMode] = useState<Mode>('signin')

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')

    const isSignUp = mode === 'signup'

    const hasEightChars = password.length >= 8 
    const hasNumber = /[0-9]/.test(password)
    const hasSpecialChar = /[^A-Za-z0-9]/.test(password)
    const isPasswordValid = hasEightChars && hasNumber && hasSpecialChar

    const isCharValid = hasEightChars;
    const isNumValid = hasNumber;
    const isSpecialCharValid = hasSpecialChar;


    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        
        if(!isSignUp){
            const response = await fetch('http://localhost:8080/signin' , {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({email, password}),
            })

            if(response.ok){
                navigate('/dashboard')
                console.log("User successfully logged in.")
            }else{
                const message = await response.text()
                console.log('Signin failed:', message)
            }

        }else{

            const response = await fetch('http://localhost:8080/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password, firstName, lastName }),
            })

            if (response.ok) {
                navigate('/dashboard')
                console.log('Account created')
            } else {
                const message = await response.text()
                console.log('Signup failed:', message)
            }
        }

    }

    return (
        <div className="auth-page">
            <h1 className="auth-title">Ledjr</h1>
            <form className="auth-card" onSubmit={handleSubmit}>

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
                        <input className="auth-input" placeholder="First Name" value={firstName} onChange={(e) => setFirstName(e.target.value)}/>
                        <input className="auth-input" placeholder="Last Name" value={lastName} onChange={(e) => setLastName(e.target.value)}/>
                    </div>
                </div>

                <input className="auth-input" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)}/>
                <input className="auth-input" type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)}/>

                 <div className={`collapsible ${isSignUp ? 'expanded' : ''}`}>
                    <div className="collapsible-inner password-hints">
                        <p className={isCharValid ? 'correct' : 'wrong'}>At least 8 characters.</p>
                        <p className={isNumValid ? 'correct' : 'wrong'}>At least 1 number.</p>
                        <p className={isSpecialCharValid ? 'correct' : 'wrong'}>At least 1 special character.</p>
                    </div>
                </div>

                <button type="submit" disabled={!isPasswordValid} className='auth-submit'>{isSignUp ? 'Sign Up' : 'Sign In'}</button>

            </form>
        </div>
    )
}

export default AuthPage
