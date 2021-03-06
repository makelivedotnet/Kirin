//
//  KirinDropbox.h
//  KirinKit
//
//  Created by James Hugman on 23/12/2011.
//  Copyright 2011 Future Platforms. All rights reserved.
//

#import <Foundation/Foundation.h>


@interface KirinDropbox : NSObject {
 
}

@property (strong, nonatomic) NSMutableDictionary * DbForId;
/**
 * Stash an object in the drop box, returning a key - the token - which can be used to look up the object some place else.
 * The token is generated by appending the prefix to a generated suffix, guarunteeing a unique token for this object. 
 * No effort is made de-duplicate the object - the token is unique per call.
 *
 */
- (NSString*) putObject:(id) object;

- (id) consumeObjectWithToken:(NSString *) token;

@end
