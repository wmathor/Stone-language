<p align = "center">
  <a href = "https://github.com/mathors/Stone-language">
    <img height="60%" width = "70%" src = "https://s1.ax1x.com/2018/12/15/FaQuNt.png">
  </a>
</p>
<p align = "center">
  <a href = "https://github.com/mathors/Stone-language">
    <img src="https://img.shields.io/badge/language-Java-brightgreen.svg">
  </a>
  <a href = "https://github.com/mathors/Stone-language">
    <img src = "https://img.shields.io/badge/Compiler-Eclipse-blue.svg">
  </a>
  <a href = "https://wmathor.com/" target = "_blank">
    <img src = "https://img.shields.io/badge/Blog-wmathor-orange.svg">
  </a>
</p>

> welcome to pull request/fork/star, thanks~
  
Maybe you just finished compiling the principle, or not, but you want to make a programming language of your own, then look at this project is right, I will design a simple scripting language in 14 days.

## Final Effect
![](https://s1.ax1x.com/2018/12/23/FyqytJ.gif#shadow)

## How To Use
First, download Stone.jar from the Code directory and import it into your IDE.

Then run the JavaRunner.java code in the chap14 package.

Stone language support function, like this
```
def fib(n) {
    if n < 2 {
        n
    } else  {
        fib(n - 1) + fib(n - 2)
    }
}
fib 33
```
Branches, loop statements are also essential
```
odd = 0
even = 0
i = 1
while i < 11 {
    if i % 2 == 0 {
        even = even + i
    } else {
        odd = odd + i
    }
    i = i + 1
}
even + odd
```

Also supports object-oriented syntax, like this
```
class Position {
    x = y = 0
    def move(nx,ny) {
        x = nx; y = ny
    }
}
p = Position.new
p.move(3, 4)
p.x = 10
print p.x + p.y
```
There are also many language-supported grammars in the Stone language, such as arrays, object inheritance, etc.
```
a = [2,3,4]
print a[1]
a[1] = "three"
print "a[1]: " + a[1]
b = [["one",1],["two",2]]
print b[1][0] + ": " + b[1][1]
```
```
class Position {
    x = y = 0
    def move(nx,ny) {
        x = nx; y = ny
    }
}
class Pos3D extends Position {
    z = 0
    def set(nx,ny,nz) {
        x = nx;y = ny;z = nz
    }
}
p = Pos3D.new
p.move(3,4)
print p.x
p.set(5,6,7)
print p.z
```

-----
## Directory 
### Part 1 Basics
#### [First Day](https://github.com/mathors/Stone-language/tree/master/First%20Day)

Introduction to machine language, assembly language, language processor overview
#### [Second Day](https://github.com/mathors/Stone-language/tree/master/Second%20Day)

Design the Stone language to determine what grammar functions the Stone language needs
#### [Third Day](https://github.com/mathors/Stone-language/tree/master/Third%20Day)

Design a lexical analyzer to introduce methods for lexical analysis through regular expressions
#### [Fourth Day](https://github.com/mathors/Stone-language/tree/master/Fourth%20Day)

Explain the abstract syntax tree and express the syntax of the Stone language through BNF
#### [Fifth Day](https://github.com/mathors/Stone-language/tree/master/Fifth%20Day)

Create a grammar interpreter with a very simple parser combination sublibrary
#### [Sixth Day](https://github.com/mathors/Stone-language/tree/master/Sixth%20Day)

Design a very basic interpreter. The interpreter will be able to actually execute programs written in the Stone language
#### [Seventh Day](https://github.com/mathors/Stone-language/tree/master/Seventh%20Day)

Enhance the functionality of the interpreter so that it can execute functions in the program and support closure syntax
#### [Eighth Day](https://github.com/mathors/Stone-language/tree/master/Eighth%20Day)

Adding call support for static methods to the interpreter, enabling the Stone language to call static methods like the Java language
#### [Ninth Day](https://github.com/mathors/Stone-language/tree/master/Ninth%20Day)

New class and object syntax for the Stone language
#### [Tenth Day](https://github.com/mathors/Stone-language/tree/master/Tenth%20Day)

Add array functionality to the Stone language

-----
### Part 2 Performance Optimization
#### [Eleventh Day](https://github.com/mathors/Stone-language/tree/master/Eleventh%20Day)

Programs should not search for variable names each time they access variables, but should first search for previously assigned numbers to improve access performance.
#### [Twelfth Day](https://github.com/mathors/Stone-language/tree/master/Twelfth%20Day)

Similarly, when a program calls a method of an object or references a field in it, it should not search its name directly, but search for the number. In addition, inline cache is added to the Stone language interpreter to further optimize performance.
#### [Thirteenth Day](https://github.com/mathors/Stone-language/tree/master/Thirteenth%20Day)

The Stone language interpreter also uses the mechanism of intermediate code interpretation (or virtual machine). The program written in the Stone language will be first converted to intermediate code (binary code), and the interpreter executes the converted intermediate code.
#### [Fourteenth Day](https://github.com/mathors/Stone-language/tree/master/Fourteenth%20Day)

Finally, to improve performance, the Stone language needs to support static data types and further optimize performance based on data types. When executing a Stone language program with a static data type, the compiler can first convert it to Java binary code and execute the program directly from the Java virtual machine. It also adds type checking to the compiler, checks for type errors before executing the program, and provides type prediction. In this way, even if the program does not explicitly declare the data type, the Stone language interpreter can speculate and specify the appropriate type.

-----
### Part 3 Advanced (self-study)
#### [Fifteenth Day](https://github.com/mathors/Stone-language/tree/master/Fifteenth%20Day)

The Stone language lexer is implemented by Java's regular expression library, and will no longer be used this way to manually design a lexer. Specifically, here will introduce the string matching program design for regular expressions.
#### [Sixteenth Day](https://github.com/mathors/Stone-language/tree/master/Sixteenth%20Day)

The parser was previously implemented using a simple library of parser combinatorial sub-libraries. From now on, some basic algorithms for parsing will be introduced. Based on LL parsing, a simple parser will be designed by hand.
#### [Seventeenth Day](https://github.com/mathors/Stone-language/tree/master/Seventh%20Day)

Briefly introduce the internal structure of the parser combination sub-library, and analyze the source code of the library
#### [Eighteenth Day](https://github.com/mathors/Stone-language/tree/master/Eighteenth%20Day)

The Stone language interpreter is implemented using the GluonJ system, which allows the Java language to perform functions similar to the open class in the Ruby language. Here are some trivial considerations when using GluonJ.
#### [Nineteenth Day](https://github.com/mathors/Stone-language/tree/master/Nineteenth%20Day)

Introduce the advantages and disadvantages of using design patterns to implement abstract syntax trees, and compare them with GluonJ.

## TODO
I will modify the syntax of the Stone language to make it support Chinese scripting language, similar to easy language.
