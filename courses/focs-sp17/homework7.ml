(* 

HOMEWORK 7

Name: 

Email:

Remarks, if any:

*)


(*
 *
 * Please fill in this file with your solutions and submit it
 *
 * The functions below are stubs that you should replace with your
 * own implementation.
 *
 * PLEASE DO NOT CHANGE THE TYPES IN THE STUBS I GIVE YOU. 
 * Doing so risks make it impossible to test your code.
 *
 * Always make sure you can #use this file before submitting it.
 * Do that in a _fresh_ OCaml shell 
 * It has to load without any errors.
 *
 *)


(*
 * Type for grammars
 *
 *)

type symbol = string

type grammar = {
  nonterms: symbol list;
  terms: symbol list;
  rules: (string * string) list;
  initial : symbol
}


(* 
 * Some sample (context-free) grammars 
 *
 *)

let anbn = {
  nonterms = ["S"];
  terms = ["a";"b"];
  rules = [("S","");
           ("S","aSb")];
  initial = "S"
}

let anbm = {
  nonterms = ["S";"T";"B"];
  terms = ["a";"b"];
  rules = [ ("S","TB");
            ("T","");
	    ("T","aTb");
	    ("B","");
	    ("B","Bb")];
  initial = "S"
}


(*
 * Here's a grammar that is _not_ context-free
 *
 * It's also harder to generate its strings
 *
 *)

let anbncn = {
  nonterms = ["S";"A";"B";"C";"X"];
  terms = ["a";"b";"c"];
  rules = [ ("S","");
            ("S","ABC");
	    ("bX","Xb");
	    ("AX","a");
	    ("aX","Xa");
	    ("XC","c");
	    ("Xc","cX");
	    ("B","XbBX");
	    ("B","");
	    ("A","AA");
	    ("C","CC")];
  initial = "S"
}




(* abbreviations *)

let map = List.map
let len = String.length
let sub = String.sub


(*
 * Utility functions 
 * 
 *)


(* check is lhs is a prefix of str *)

let prefix lhs str =
  lhs = (sub str 0 (len lhs))


(* replace prefix lhs of str with rhs *)

let replace lhs str rhs =
  let l = len lhs in
  rhs ^ (sub str l (len str - l))


(* try to apply rule (lhs,rhs) to str (assuming prefix prf) *)

let apply_rule prf (lhs,rhs) str =
  if len str < len lhs 
    then []
  else if prefix lhs str
    then [prf^(replace lhs str rhs)]
  else []


(* try to apply every rule in rs to str *)

let rec apply_rules rs str =
  let rec loop prefix str = 
    if str = "" then []
    else let rest = loop (prefix^(sub str 0 1)) (sub str 1 (len str -1))  in
       (List.fold_left (fun res r -> res@(apply_rule prefix r str)) [] rs)@rest  in
  loop "" str


(*
 * Perform an iteratively deepening depth-first search of the rewrite 
 * tree
 *
 *)

module StringSet = Set.Make(String)

let dfs_path maxdepth maxwidth grammar target =
  let lt = len target  in
  let rec loop q seen =
    if q = []
      then []
    else let ((path,d)::q) = q in
         let (str::_) = path in
	 if len str > maxwidth
	   then loop q seen
         else if len str = lt && str = target
	   then path
	 else if StringSet.mem str seen
	   then loop q seen
	 else if d > maxdepth
	   then loop q (StringSet.add str seen)
	 else (* let _ = (print_string str; print_newline()) in *)
	      let new_strs = apply_rules grammar.rules str in
	      let new_strs_d = map (fun x -> (x::path,d+1)) new_strs in
	      let q = (new_strs_d)@q in
	      loop q (StringSet.add str seen) in
  loop [([grammar.initial],0)] StringSet.empty

let idfs_path maxdepth grammar target =
  let rec loop n =
    let _ = Printf.printf "Searching (depth %02d, max width %d)" n n in
    let _ = print_newline ()  in
    if n > maxdepth
      then []
    else match dfs_path n n grammar target with
         | [] -> loop (n+1)
	 | path -> path  in
  loop 1


(* 
 * Check if a grammar is well-formed 
 *
 *)

let checkGrammar grammar = 
  let _ = List.iter (fun x -> if String.length x != 1 then failwith ("symbol "^x^" not a single character") else ()) grammar.nonterms  in
  let _ = List.iter (fun x -> if String.length x != 1 then failwith ("symbol "^x^" not a single character") else ()) grammar.terms  in
  let _ = List.iter (fun (p,q) -> if String.length p < 1 then failwith "rule with empty left-hand side" else ()) grammar.rules  in
  let _ = if List.mem grammar.initial grammar.nonterms then () else failwith "start symbol not a nonterminal"  in
  ()



(*
 * Try to generate a string for a given grammar 
 * 
 *)

let generate md grammar str =
  let _ = checkGrammar grammar in
  let print pre str = (print_string pre;
                       print_string str;
		       print_newline ())  in
  let rec rev_print path =
    match path with
    | [] -> ()
    | [s] -> print "   " s
    | s::ss -> (rev_print ss; print "-> " s)  in
  let path = idfs_path md grammar str  in
  let _ = rev_print path  in
  path != []
  


(* 
 * QUESTION 1
 *
 *)


let dummy_grammar = {
  nonterms = [];
  terms = [];
  rules = [];
  initial = ""
} 


let q1_ambncmn : grammar = dummy_grammar 


let q1_amcmnbn : grammar = dummy_grammar


let q1_ambncm : grammar = dummy_grammar


let q1_eqnum : grammar = dummy_grammar


let q1_doublenum = dummy_grammar



(* 
 * QUESTION 2 
 *
 *)


(* Type for DFAs *)

type 'a fa = { states: 'a list;
               alphabet: char list;
               delta: ('a * char * 'a) list;
               start : 'a;
               final : 'a list }


(* A dfa that accepts all strings with a multiple of three
 * number of as *)

let dfaThreeA = { 
  states = ["S";"1";"2"];
  alphabet = ['a';'b'];
  delta = [("S",'a',"1");
           ("1",'a',"2");
	   ("2",'a',"S");
	   ("S",'b',"S");
	   ("1",'b',"1");
	   ("2",'b',"2")];
  start = "S";
  final = ["S"]
} 



let faGrammar (fa:string fa):grammar = failwith "faGrammar not implemented"



(*
 * QUESTION 3 
 *
 *)
  

let q3_powers2 : grammar = dummy_grammar

let q3_dup : grammar = dummy_grammar
