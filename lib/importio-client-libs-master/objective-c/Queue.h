//
//  Queue.h
//  iOS Client Library
//
//  Created by dev on 23/09/2013.
//  Copyright (c) 2013 importio. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface Queue : NSMutableArray {
    NSCondition* conditionLock;
    NSMutableArray* underlyingArray;
}

- (void) enqueue: (id)item;
- (id) get;
- (id) peek;

@end

