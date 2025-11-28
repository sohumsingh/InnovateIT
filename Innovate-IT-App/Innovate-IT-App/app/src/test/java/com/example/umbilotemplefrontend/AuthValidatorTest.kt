package com.example.umbilotemplefrontend

import com.example.umbilotemplefrontend.utils.AuthValidator
import org.junit.Assert.*
import org.junit.Test

class AuthValidatorTest {

	@Test
	fun emailValidation_isCorrect() {
		assertTrue(AuthValidator.isEmailValid("altaaf@gmail.com"))
		assertTrue(AuthValidator.isEmailValid("user.name+tag@domain.co"))
		assertFalse(AuthValidator.isEmailValid("invalid"))
		assertFalse(AuthValidator.isEmailValid("user@domain"))
		assertFalse(AuthValidator.isEmailValid(""))
	}

	@Test
	fun passwordValidation_isCorrect() {
		// Meets strong regex: at least 8, digit, lower, upper, special
		assertTrue(AuthValidator.isPasswordValid("Aa1!aaaa"))
		assertFalse(AuthValidator.isPasswordValid("1704"))
		assertFalse(AuthValidator.isPasswordValid(""))
	}

	@Test
	fun passwordBasicValidation_isCorrect() {
		assertTrue(AuthValidator.isPasswordValidBasic("12345678"))
		assertFalse(AuthValidator.isPasswordValidBasic("1234567"))
	}

	@Test
	fun nameValidation_isCorrect() {
		assertTrue(AuthValidator.isNameValid("Altaaf"))
		assertTrue(AuthValidator.isNameValid("John Doe"))
		assertFalse(AuthValidator.isNameValid(""))
		assertFalse(AuthValidator.isNameValid("A"))
		assertFalse(AuthValidator.isNameValid("Name#With$Symbols"))
	}

	@Test
	fun passwordMatchValidation_isCorrect() {
		assertTrue(AuthValidator.doPasswordsMatch("password", "password"))
		assertFalse(AuthValidator.doPasswordsMatch("1704", "2019"))
	}

	@Test
	fun phoneValidation_isCorrect() {
		// Accepts blank per implementation
		assertTrue(AuthValidator.isPhoneNumberValid(""))
		assertTrue(AuthValidator.isPhoneNumberValid("+27821234567"))
		assertTrue(AuthValidator.isPhoneNumberValid("0821234567"))
		assertFalse(AuthValidator.isPhoneNumberValid("123"))
		assertFalse(AuthValidator.isPhoneNumberValid("phone123"))
	}

	@Test
	fun passwordStrength_isClassifiedCorrectly() {
		// Too short -> WEAK
		assertEquals(AuthValidator.PasswordStrength.WEAK, AuthValidator.getPasswordStrength("Aa1!a"))

		// Medium: length >= 8 but missing some categories
		assertEquals(AuthValidator.PasswordStrength.MEDIUM, AuthValidator.getPasswordStrength("aaaaaaaa1")) // lower + digit
		assertEquals(AuthValidator.PasswordStrength.MEDIUM, AuthValidator.getPasswordStrength("AAAAAAA1!")) // upper + digit + special

		// Strong: multiple categories and/or length >= 12 with variety
		assertEquals(AuthValidator.PasswordStrength.STRONG, AuthValidator.getPasswordStrength("Aa1!aaaaaa"))
		assertEquals(AuthValidator.PasswordStrength.STRONG, AuthValidator.getPasswordStrength("Aa1!aaaaaaaaaa")) // length >= 12 adds score
	}

	@Test
	fun passwordErrorMessages_areAccurate() {
		assertEquals("Password cannot be empty", AuthValidator.getPasswordErrorMessage(""))
		assertEquals("Password must be at least 8 characters", AuthValidator.getPasswordErrorMessage("1234567"))
		assertEquals("Password must contain at least one digit", AuthValidator.getPasswordErrorMessage("AAAAaaaa!"))
		assertEquals("Password must contain at least one lowercase letter", AuthValidator.getPasswordErrorMessage("AAAA1111!"))
		assertEquals("Password must contain at least one uppercase letter", AuthValidator.getPasswordErrorMessage("aaaa1111!"))
		assertEquals("Password must contain at least one special character", AuthValidator.getPasswordErrorMessage("Aaaa1111"))
		// Valid password returns null
		assertNull(AuthValidator.getPasswordErrorMessage("Aa1!aaaa"))
	}
}
