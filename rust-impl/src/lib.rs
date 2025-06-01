use std::ffi::{CStr, CString};
use std::os::raw::c_char;

#[no_mangle] // <- this is necessary because without it the name will change
pub extern "C" // <- also necessary
fn add_numbers(n1: i32, n2: i32) -> i32 {
    n1 + n2
}

#[no_mangle]
pub unsafe extern "C" fn print_str(str: *const c_char) {
    let str = CStr::from_ptr(str);
    println!("RUST READING: {}", str.to_str().unwrap());
}

#[no_mangle]
pub unsafe extern "C" fn greeting() -> *const c_char {
    let s = CString::new("Hello from Rust!").unwrap();
    s.into_raw()
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn test_greeting() {
        unsafe {
            let t = greeting() ;
            println!("{:?}", CStr::from_ptr(t));
        }
    }
}
