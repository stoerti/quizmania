import {useUsername} from "../hooks/useUsername.ts";
import {useNavigate} from "react-router";
import React, {useEffect} from "react";

/**
 * component without UI, just to encapsulate the logout process.
 */
const LogoutPage = () => {
  const {removeUsername} = useUsername();
  const navigate = useNavigate();

  useEffect(() => {
    console.log('logging out...')
    removeUsername()
    navigate('/')
  }, []);

  return <>logging out...</>
}

export default LogoutPage;
