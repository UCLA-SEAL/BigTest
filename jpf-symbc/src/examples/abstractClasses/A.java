/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package abstractClasses;

abstract class A  {
	int a;
	void m () {
		System.out.println(" m in A");
	}
	void n () {
		System.out.println("n in A");
	}
	void f() {
		System.out.println("f in A");
		if(a>10) {
			System.out.println("f in A10");
		}else {
			System.out.println("f in A-10");		
		}
		//	m ();
	//	n ();
	}
}